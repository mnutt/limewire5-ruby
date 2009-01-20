class AddDashboardPositions < ActiveRecord::Migration
  def self.up
    create_table :dashboard_positions do |t|
      t.integer :column
      t.integer :list_position
      t.string :name
      t.timestamps
    end
  end

  def self.down
    drop_table :dashboard_positions
  end
end
