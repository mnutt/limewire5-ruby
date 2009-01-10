ROOT = File.expand_path(File.join(Dir.pwd, 'scripting', 'rails'))
$LOAD_PATH.unshift(ROOT)
Dir.chdir(ROOT)
require 'rubygems'
Gem.path.unshift(File.expand_path(ROOT+"/vendor/gems"))
Gem.refresh

require 'core'
require "#{ROOT}/config/boot"
require 'commands/server'
