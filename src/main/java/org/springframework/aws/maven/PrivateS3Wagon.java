package org.springframework.aws.maven;

import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * I threw up im my mouth a little...ok a lot...while writing this.
 */
public class PrivateS3Wagon extends SimpleStorageServiceWagon {

    RestS3Service service;
    String bucket;
    String basedir;

    @Override
    protected void connectToRepository(Repository source, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider)
            throws AuthenticationException {
        try {
            Jets3tProperties jets3tProperties = new Jets3tProperties();
            if (proxyInfoProvider != null) {
                ProxyInfo proxyInfo = proxyInfoProvider.getProxyInfo("http");
                if (proxyInfo != null) {
                    jets3tProperties.setProperty("httpclient.proxy-autodetect", "false");
                    jets3tProperties.setProperty("httpclient.proxy-host", proxyInfo.getHost());
                    jets3tProperties.setProperty("httpclient.proxy-port", new Integer(proxyInfo.getPort()).toString());
                }
            }
            this.service = new RestS3Service(getCredentials(authenticationInfo), "mavens3wagon", null, jets3tProperties);
        } catch (S3ServiceException e) {
            throw new AuthenticationException("Cannot authenticate with current credentials", e);
        }
        this.bucket = source.getHost();
        this.basedir = getBaseDir(source);
        super.connectToRepository(source, authenticationInfo, proxyInfoProvider);
    }

    private AWSCredentials getCredentials(AuthenticationInfo authenticationInfo) throws AuthenticationException {
        if (authenticationInfo == null) {
            return null;
        }
        String accessKey = authenticationInfo.getUserName();
        String secretKey = authenticationInfo.getPassphrase();
        if (accessKey == null || secretKey == null) {
            throw new AuthenticationException("S3 requires a username and passphrase to be set");
        }
        return new AWSCredentials(accessKey, secretKey);
    }

    private String getBaseDir(Repository source) {
        StringBuilder sb = new StringBuilder(source.getBasedir());
        sb.deleteCharAt(0);
        if (sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }

    private void buildDestinationPath(String destination) throws S3ServiceException {
        S3Object object = new S3Object(this.basedir + destination + "/");
        object.setAcl(AccessControlList.REST_CANNED_PRIVATE);
        object.setContentLength(0);
        this.service.putObject(this.bucket, object);
        int index = destination.lastIndexOf('/');
        if (index != -1) {
            buildDestinationPath(destination.substring(0, index));
        }
    }

    private String getDestinationPath(String destination) {
        return destination.substring(0, destination.lastIndexOf('/'));
    }

    @Override
    protected void putResource(File source, String destination, TransferProgress progress) throws S3ServiceException, IOException {
        buildDestinationPath(getDestinationPath(destination));
        S3Object object = new S3Object(this.bucket + destination);
        object.setAcl(AccessControlList.REST_CANNED_PRIVATE);
        object.setDataInputFile(source);
        object.setContentLength(source.length());

        InputStream in = null;
        try {
            this.service.putObject(this.bucket, object);

            in = new FileInputStream(source);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                progress.notify(buffer, length);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Nothing possible at this point
                }
            }
        }
    }
}
