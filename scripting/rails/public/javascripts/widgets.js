$(function() {
  $('#widgets_container ul').sortable({
    handle: 'div.handle',
    opacity: 0.8,
    connectWith: ['.widgets'],
    stop: function() {
      console.log("reordered");
    }
  });
});