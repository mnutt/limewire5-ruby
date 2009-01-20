# This is the file containing the mapping of the plugin's routes.  This file is evaluated in the
# app's top-level routes.rb after all other routes.  #plugin_map is yielded by
# map.with_options(:path_prefix => 'stats')

# Map a controller and action to respond to /statsplugin_map.root :controller => 'stats', :action => 'index'

# Like mapping ':controller/:action', this catches most things
plugin_map.backup ':action/:id', :controller => 'stats'

# Add this route to point to your widget action if you want to display a widget on the homepage
plugin_map.widget "/widget", :controller => 'stats', :action => 'widget'
