class AssetsController < ApplicationController
  def show
    path = params[:path].join("/")
    plugin_name = params[:plugin]
    full_path = File.join(PLUGIN_ROOT, plugin_name, 'public', path)
    raise "Not in plugin directory!" unless (File.expand_path(full_path) =~ /^#{File.expand_path(PLUGIN_ROOT)}/)

    # Find the content-type using the file extension and mongrel's MIME_TYPES file
    dot_at = path.rindex('.')
    content_type = Mongrel::DirHandler::MIME_TYPES[path[dot_at .. -1]] if dot_at

    if File.exist?(full_path)
      send_file(full_path, :type => content_type)
    else
      render :status => 404, :text => "Could not find file: #{full_path}"
    end
  end
end
