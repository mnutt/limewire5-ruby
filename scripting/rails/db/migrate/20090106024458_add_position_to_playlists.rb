class AddPositionToPlaylists < ActiveRecord::Migration
  def self.up
    add_column :playlists, :list_position, :integer
  end

  def self.down
    remove_column :playlists, :list_position
  end
end
