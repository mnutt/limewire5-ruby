$(function() {
  $('#widgets').sortable({
    handle: 'div.handle',
    opacity: 0.8,
    stop: function() {
      console.log("reordered");
    }
  });
});