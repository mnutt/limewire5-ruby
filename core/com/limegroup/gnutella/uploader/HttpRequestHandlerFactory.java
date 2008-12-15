package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.uploader.authentication.HttpRequestFileListProvider;

public interface HttpRequestHandlerFactory {

    public FileRequestHandler createFileRequestHandler(HttpRequestFileListProvider fileRequestFileListProvider, boolean requiresAuthentication);
    
    public BrowseRequestHandler createBrowseRequestHandler(HttpRequestFileListProvider browseRequestFileListProvider, boolean requiresAuthentication);

    public FreeLoaderRequestHandler createFreeLoaderRequestHandler();

    // TODO move LimitReachedRequestHandler into FileRequestHandler
    public LimitReachedRequestHandler createLimitReachedRequestHandler(HTTPUploader uploader);
    
    public HttpPushRequestHandler createPushProxyRequestHandler();

}