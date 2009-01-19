class AssetsController < ApplicationController
  def show
    path = params[:path].join("/")
    plugin_name = params[:plugin]
    full_path = File.join(PLUGIN_ROOT, plugin_name, 'public', path)
    raise "Not in plugin directory!" unless (File.expand_path(full_path) =~ /^#{File.expand_path(PLUGIN_ROOT)}/)
    
    if File.exist?(full_path)
      send_file(full_path)
    else
      render :status => 404, :text => "Could not find file: #{full_path}"
    end
  end
end
