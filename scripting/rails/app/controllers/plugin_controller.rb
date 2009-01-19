class PluginController < ApplicationController
  def self.view_paths
    [File.expand_path("#{PLUGIN_ROOT}/#{self.plugin_name}/views")]
  end

  def self.plugin_name
    self.to_s.split("Controller").first.downcase
  end

  def _plugin_name
    self.class.plugin_name
  end
  helper_method :_plugin_name
end
