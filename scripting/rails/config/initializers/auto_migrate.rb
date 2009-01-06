# Auto migrate rails
ActiveRecord::Migrator.migrate("#{RAILS_ROOT}/db/migrate/", nil)
require 'active_record/schema_dumper'
File.open("#{RAILS_ROOT}/db/schema.rb", "w") do |file|
  ActiveRecord::SchemaDumper.dump(ActiveRecord::Base.connection, file)
end
