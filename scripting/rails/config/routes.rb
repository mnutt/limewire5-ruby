ActionController::Routing::Routes.draw do |map|

  map.file "/library/:sha1.mp3", :controller => 'library', :action => 'show'
 
  # Cloud Player
  map.resources :playlists, :path_prefix => "/cloud"
  map.tracks "/cloud/tracks.json", :controller => 'cloud', :action => 'tracks'
  map.new_search "/search/q/:query", :controller => 'search', :action => 'perform'
  map.search_control "/search/:guid/:query", :controller => 'search', :action => 'control'
  map.cloud "/cloud", :controller => 'cloud', :action => 'index'
  map.resources :downloads
  map.download '/download/:magnet', :controller => 'library', :action => 'download'
  map.resources :galleries, :collection => {:all => :get}

  map.assets '/assets/:plugin/*path', :controller => 'assets', :action => 'show'

  map.root :controller => 'home', :action => 'index'

  Dir.glob("#{RAILS_ROOT}/plugins/*/routes.rb").each do |routes_file|
    plugin_name = routes_file.split("/").reverse[1]
    map.with_options(:path_prefix => plugin_name) do |plugin_map|
      eval(File.open(routes_file).read)
    end
  end
end
