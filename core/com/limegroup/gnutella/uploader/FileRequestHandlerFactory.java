package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.uploader.authentication.HttpRequestFileListProvider;

interface FileRequestHandlerFactory {

    FileRequestHandler createFileRequestHandler(
            HttpRequestFileListProvider fileListProvider, boolean requiresAuthentication);

}