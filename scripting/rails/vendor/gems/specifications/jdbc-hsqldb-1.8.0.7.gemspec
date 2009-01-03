# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{jdbc-hsqldb}
  s.version = "1.8.0.7"

  s.required_rubygems_version = nil if s.respond_to? :required_rubygems_version=
  s.authors = ["Nick Sieger, Ola Bini and JRuby contributors"]
  s.cert_chain = nil
  s.date = %q{2007-11-05}
  s.description = %q{Install this gem and require 'hsqldb' within JRuby to load the driver.}
  s.email = %q{nick@nicksieger.com, ola.bini@gmail.com}
  s.extra_rdoc_files = ["Manifest.txt", "README.txt", "LICENSE.txt"]
  s.files = ["Manifest.txt", "Rakefile", "README.txt", "LICENSE.txt", "lib/hsqldb-1.8.0.7.jar", "lib/jdbc", "lib/jdbc/hsqldb.rb"]
  s.has_rdoc = true
  s.homepage = %q{http://jruby-extras.rubyforge.org/ActiveRecord-JDBC}
  s.rdoc_options = ["--main", "README.txt"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new("> 0.0.0")
  s.rubyforge_project = %q{jruby-extras}
  s.rubygems_version = %q{1.3.1}
  s.summary = %q{HSQLDB JDBC driver for Java and HSQLDB/ActiveRecord-JDBC.}

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 1

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
