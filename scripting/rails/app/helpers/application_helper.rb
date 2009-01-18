# Methods added to this helper will be available to all templates in the application.
module ApplicationHelper
  def javascript_include(*scripts)
    @javascript_includes ||= []
    @javascript_includes << scripts
  end

  def print_dollars(cents)
    if cents.to_s.include?("-")
      decimals = cents.to_s.split("-").last + 1
    elsif cents % 0.01 > 0.0
      decimals = 3
    elsif cents % 0.001 > 0.0
      decimals = 4
    else
      decimals = 2
    end

    sprintf("%6.#{decimals}f", cents)
  end
      
end
