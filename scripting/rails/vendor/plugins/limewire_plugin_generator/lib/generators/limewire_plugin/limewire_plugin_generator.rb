class LimewirePluginGenerator < Rails::Generator::NamedBase
  def manifest
    record do |m|
      # Check for class naming collisions.
      m.class_collisions "#{class_name}Controller", "#{class_name}ControllerTest", "#{class_name}Helper"

      # Controller, helper, views, and test directories.
      m.directory File.join('plugins', file_name, 'controllers', class_path)
      m.directory File.join('plugins', file_name, 'helpers', class_path)
      m.directory File.join('plugins', file_name, 'views', class_path, file_name)
      m.directory File.join('plugins', file_name, 'test/functional', class_path)

      # Controller class, functional test, and helper class.
      m.template 'controller.rb',
                  File.join('plugins', file_name, 'controllers',
                            class_path,
                            "#{file_name}_controller.rb")

      m.template 'functional_test.rb',
                  File.join('plugins', file_name, 'test/functional',
                            class_path,
                            "#{file_name}_controller_test.rb")

      m.template 'helper.rb',
                  File.join('plugins', file_name, 'helpers',
                            class_path,
                            "#{file_name}_helper.rb")

      m.template 'routes.rb',
                  File.join('plugins', file_name, 'routes.rb'),
                  :assigns => { :name => file_name }

      # View template for each action.
      actions.each do |action|
        path = File.join('plugins', file_name, 'views', class_path, file_name, "#{action}.html.erb")
        m.template 'view.html.erb', path,
          :assigns => { :action => action, :path => path }
      end
    end
  end
end
