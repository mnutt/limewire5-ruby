class PluginController < ApplicationController
  def self.controller_path
    "" # Use ./views for the view path, rather than ./{controller}/views
  end

  def self.view_paths
    plugin_name = self.to_s.split("Controller").first.downcase
    [File.expand_path("#{PLUGIN_ROOT}/#{plugin_name}/views")]
  end
end
