#!/bin/env ruby

RELEASE_BASE = "deploy/releases"

# Set the release dir
puts "Creating the release dir..."
date = Time.now.strftime('%Y-%m-%d')
release_dir = File.join(RELEASE_BASE, date)
while(File.exist?(release_dir))
  release_dir << "0"
end
puts "Release created in #{release_dir}"

# Add the portable LimeWire skeleton
puts "Adding the portable LimeWire skeleton..."
if File.exist?("skeleton/LimeWire.zip")
  `unzip -d #{release_dir} deploy/LimeWire.zip`
elsif File.exist?("skeleton/LimeWire")
  `cp -R deploy/LimeWire #{release_dir}/`
else
  puts "LimeWire Portable skeleton not found.  Get it from http://wiki.limewire.org/... and put it in ./deploy/ directory"
  exit(1)
end

puts "Building LimeWire jars..."
# Build LimeWire
puts `ant -f portable.xml`

puts "Building launch jars..."
# Build launch jars
puts `ant -f portable.xml launch`

# Copy jars to skeleton
puts "Copying jars to skeleton..."
puts `cp deploy/*.jar #{release_dir}/LimeWire.app/Contents/Resources/Java/`
raise "Could not copy jars" unless File.exist?("#{release_dir}/LimeWire.app/Contents/Resources/Java/LimeWire.jar")

# Optionally copy rails to skeleton
scripting_dir = "#{release_dir}/LimeWire.app/Contents/Resources/Java/scripting"
if(File.exist?(scripting_dir))
  `rm -Rf #{scripting_dir}`
end

puts "Compressing archive..."
puts `cd #{release_dir} && zip -r portable-limewire.zip *`
