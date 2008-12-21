# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{merb-freezer}
  s.version = "0.9.5"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Matt Aimonetti"]
  s.autorequire = %q{merb-freezer}
  s.date = %q{2008-08-26}
  s.default_executable = %q{frozen-merb}
  s.description = %q{Merb plugin that let's you freeze Merb}
  s.email = %q{mattaimonetti@gmail.com}
  s.executables = ["frozen-merb"]
  s.extra_rdoc_files = ["README", "LICENSE", "TODO"]
  s.files = ["LICENSE", "README", "Rakefile", "TODO", "lib/merb-freezer", "lib/merb-freezer/freezer.rb", "lib/merb-freezer/freezer_mode.rb", "lib/merb-freezer/merbtasks.rb", "lib/merb-freezer.rb", "spec/merb-freezer_spec.rb", "spec/spec_helper.rb", "bin/frozen-merb"]
  s.has_rdoc = true
  s.homepage = %q{http://merbivore.com}
  s.require_paths = ["lib"]
  s.rubyforge_project = %q{merb}
  s.rubygems_version = %q{1.3.1}
  s.summary = %q{Merb plugin that let's you freeze Merb}

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 2

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<merb-core>, [">= 0.9.5"])
    else
      s.add_dependency(%q<merb-core>, [">= 0.9.5"])
    end
  else
    s.add_dependency(%q<merb-core>, [">= 0.9.5"])
  end
end
