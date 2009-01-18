ROOT = File.expand_path(File.join(Dir.pwd, 'scripting', 'rails'))
$LOAD_PATH.unshift(ROOT)

# FIXME: This is unsafe.  Do not use for things that matter.
class PersistentStore
  @@store = {}
  def self.[]=(key, value)
    @@store[key] = value
  end

  def self.[](key)
    @@store[key]
  end

  def self.to_yaml
    @@store.to_yaml
  end
end

Dir.chdir(ROOT)
require 'rubygems'
Gem.path.unshift(File.expand_path(ROOT+"/vendor/gems"))
Gem.refresh

require '../core'
require "#{ROOT}/config/boot"
require 'commands/server'
