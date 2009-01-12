jQuery(document).ready(function(){
  var submitSearch = function() {
    console.log("results: ");
    $.getJSON('/search/q/' + $('#q').val(), function(search) {
      guid = search.guid;
      times_refreshed = 0;
      _loadingSearch = false;
      $.periodic(function(controller) {
	if(_loadingSearch) { _loadingSearch = false; return true; }
        if(times_refreshed && times_refreshed > 20) { controller.stop(); }

	_loadingSearch = true;
	times_refreshed ? times_refreshed++ : times_refreshed = 1;

	$.getJSON("/search/" + guid + "/results", function(realData) {
	  $("#loading").hide();
	  results = eval(realData).results;
		    //console.log(results.length);
	    $.each(results, function(index, result) {
	      if($('#'+result.sha1).length == 0) {
		$('#results').append("<li id="+result.sha1+">"+result.filename+"</li>");
	      }
	      if($('#results li').length > 50) {
		controller.stop();
	      }
	  });

	  _loadingSearch = false;
	});
	return true;
      }, {frequency: 1});
    });
  };

  $('#search_form').submit(submitSearch);
  $('#search_submit').click(submitSearch);
});