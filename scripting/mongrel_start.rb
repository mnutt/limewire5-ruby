ROOT = File.expand_path(File.join(Dir.pwd , 'scripting', 'merb'))

require 'rubygems'
Gem.clear_paths
$BUNDLE=true
Gem.path.unshift(File.expand_path(ROOT+"/gems"))
Gem.refresh
require 'merb-core'

Merb.start_environment(:adapter => :mongrel, 
                       :merb_root => ROOT,
                       :log_file => "log/development.log")
