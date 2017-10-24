 /*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.build.aws.maven;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Mimetypes;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * An implementation of the Maven Wagon interface that allows you to access the Amazon S3 service. URLs that reference
 * the S3 service should be in the form of <code>s3://bucket.name</code>. As an example
 * <code>s3://static.springframework.org</code> would put files into the <code>static.springframework.org</code> bucket
 * on the S3 service.
 * <p/>
 * This implementation uses the <code>username</code> and <code>passphrase</code> portions of the server authentication
 * metadata for credentials.
 */
public final class PrivateS3Wagon extends AbstractWagon {

    private static final String KEY_FORMAT = "%s%s";

    private static final String RESOURCE_FORMAT = "%s(.*)";

    private volatile AmazonS3 amazonS3;

    private volatile String bucketName;

    private volatile String baseDirectory;

    /**
     * Creates a new instance of the wagon
     */
    public PrivateS3Wagon() {
        super(true);
    }

    PrivateS3Wagon(AmazonS3 amazonS3, String bucketName, String baseDirectory) {
        super(true);
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
        this.baseDirectory = baseDirectory;
    }

    @Override
    protected void connectToRepository(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider)
        throws AuthenticationException {
        if (this.amazonS3 == null) {
            ClientConfiguration clientConfiguration = S3Utils.getClientConfiguration(proxyInfoProvider);


            this.bucketName = S3Utils.getBucketName(repository);
            this.baseDirectory = S3Utils.getBaseDirectory(repository);

            // if AuthenticationInfo is null or empty, use the default provider from AWS SDK
            boolean defaultCredProvider = false;
            if (authenticationInfo == null) {
                defaultCredProvider = true;
            } else if (authenticationInfo.getUserName() == null &&
                       authenticationInfo.getPassword() == null &&
                       authenticationInfo.getPassphrase() == null &&
                       (authenticationInfo.getPrivateKey() == null ||
                        authenticationInfo.getPrivateKey().equals(""))) {
                defaultCredProvider = true;
            }

            if (defaultCredProvider) {
                this.amazonS3 = new AmazonS3Client(new DefaultAWSCredentialsProviderChain(), clientConfiguration);
            } else {
                AWSCredentials awsCredentials = new AuthenticationInfoAWSCredentials(authenticationInfo);
                this.amazonS3 = new AmazonS3Client(awsCredentials, clientConfiguration);
            }

            try {
                com.amazonaws.regions.Region region = parseRegion(new DefaultAwsRegionProviderChain().getRegion());
                if (!region.getPartition().equals("aws")) {
                    this.amazonS3.setRegion(region);
                } else {
                    detectEndpointFromBucket();
                }
            } catch (AmazonClientException e) {
                detectEndpointFromBucket();
            }
        }
    }

    private void detectEndpointFromBucket() {
        String location = this.amazonS3.getBucketLocation(this.bucketName);

        try {
            Region region = Region.fromLocationConstraint(this.amazonS3.getBucketLocation(this.bucketName));
            this.amazonS3.setEndpoint(region.getEndpoint());
        } catch (IllegalArgumentException e) {
            this.amazonS3.setRegion(parseRegion(location));
        }
    }

    private com.amazonaws.regions.Region parseRegion(String region) {
        return com.amazonaws.regions.Region.getRegion(Regions.fromName(region));
    }
    
    @Override
    protected void disconnectFromRepository() {
        this.amazonS3 = null;
        this.bucketName = null;
        this.baseDirectory = null;
    }

    @Override
    protected boolean doesRemoteResourceExist(String resourceName) {
        try {
            getObjectMetadata(resourceName);
            return true;
        } catch (AmazonServiceException e) {
            return false;
        }
    }

    @Override
    protected boolean isRemoteResourceNewer(String resourceName, long timestamp) throws ResourceDoesNotExistException {
        try {
            Date lastModified = getObjectMetadata(resourceName).getLastModified();
            return lastModified == null ? true : lastModified.getTime() > timestamp;
        } catch (AmazonServiceException e) {
            throw new ResourceDoesNotExistException(String.format("'%s' does not exist", resourceName), e);
        }
    }

    @Override
    protected List<String> listDirectory(String directory) throws ResourceDoesNotExistException {
        List<String> directoryContents = new ArrayList<String>();

        try {
            String prefix = getKey(directory);
            Pattern pattern = Pattern.compile(String.format(RESOURCE_FORMAT, prefix));

            ListObjectsRequest listObjectsRequest = new ListObjectsRequest() //
            .withBucketName(this.bucketName) //
            .withPrefix(prefix) //
            .withDelimiter("/");

            ObjectListing objectListing;

            objectListing = this.amazonS3.listObjects(listObjectsRequest);
            directoryContents.addAll(getResourceNames(objectListing, pattern));

            while (objectListing.isTruncated()) {
                objectListing = this.amazonS3.listObjects(listObjectsRequest);
                directoryContents.addAll(getResourceNames(objectListing, pattern));
            }

            return directoryContents;
        } catch (AmazonServiceException e) {
            throw new ResourceDoesNotExistException(String.format("'%s' does not exist", directory), e);
        }
    }

    @Override
    protected void getResource(String resourceName, File destination, TransferProgress transferProgress) throws TransferFailedException,
        ResourceDoesNotExistException {
        InputStream in = null;
        OutputStream out = null;
        try {
            S3Object s3Object = this.amazonS3.getObject(this.bucketName, getKey(resourceName));

            in = s3Object.getObjectContent();
            out = new TransferProgressFileOutputStream(destination, transferProgress);

            IoUtils.copy(in, out);
        } catch (AmazonServiceException e) {
            throw new ResourceDoesNotExistException(String.format("'%s' does not exist", resourceName), e);
        } catch (FileNotFoundException e) {
            throw new TransferFailedException(String.format("Cannot write file to '%s'", destination), e);
        } catch (IOException e) {
            throw new TransferFailedException(String.format("Cannot read from '%s' and write to '%s'", resourceName, destination), e);
        } finally {
            IoUtils.closeQuietly(in, out);
        }
    }

    @Override
    protected void putResource(File source, String destination, TransferProgress transferProgress) throws TransferFailedException,
        ResourceDoesNotExistException {
        String key = getKey(destination);

        InputStream in = null;
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(source.length());
            objectMetadata.setContentType(Mimetypes.getInstance().getMimetype(source));

            in = new TransferProgressFileInputStream(source, transferProgress);

            this.amazonS3.putObject(new PutObjectRequest(this.bucketName, key, in, objectMetadata));
        } catch (AmazonServiceException e) {
            throw new TransferFailedException(String.format("Cannot write file to '%s'", destination), e);
        } catch (FileNotFoundException e) {
            throw new ResourceDoesNotExistException(String.format("Cannot read file from '%s'", source), e);
        } finally {
            IoUtils.closeQuietly(in);
        }
    }

    private ObjectMetadata getObjectMetadata(String resourceName) {
        return this.amazonS3.getObjectMetadata(this.bucketName, getKey(resourceName));
    }

    private String getKey(String resourceName) {
        return String.format(KEY_FORMAT, this.baseDirectory, resourceName);
    }

    private List<String> getResourceNames(ObjectListing objectListing, Pattern pattern) {
        List<String> resourceNames = new ArrayList<String>();

        for (String commonPrefix : objectListing.getCommonPrefixes()) {
            resourceNames.add(getResourceName(commonPrefix, pattern));
        }

        for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
            resourceNames.add(getResourceName(s3ObjectSummary.getKey(), pattern));
        }

        return resourceNames;
    }

    private String getResourceName(String key, Pattern pattern) {
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return key;
    }

}
