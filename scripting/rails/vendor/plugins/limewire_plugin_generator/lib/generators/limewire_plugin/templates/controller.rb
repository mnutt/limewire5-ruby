class <%= class_name %>Controller < PluginController
<% for action in actions -%>
  def <%= action %>
  end

<% end -%>
end
