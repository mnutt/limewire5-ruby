jQuery(document).ready(function(){
  $('#start_backup').submit(function() {
    var keys = $("#start_backup").serializeArray();
    $.post("/backup/create", keys, function(data) {
      $("#status").show();
      $.periodic(function(controller) {
	$.getJSON("/backup/status", function(status) {
	  status = eval(status);
	  $('#current_file').text(status.current_file);
	  $('#percentage').text("Uploading file " + status.current_index + " / " + status.total_files);
	  var file_percent_complete = status.file_bytes_written / status.current_file_size * 100;
	  $('#file_progress_indicator').css({ width: file_percent_complete+"%" });
	  var total_percent_complete = status.total_bytes_written / status.library_size * 100;
	  $('#total_progress_indicator').css({ width: total_percent_complete+"%" });
	});
	return true;
      }, {frequency: 1.0});
    });
    return false;
  });

  $("#stop_backup").click(function() {
    $.post("/backup/stop", {}, function(data) {
	// do something
    });
  });
});