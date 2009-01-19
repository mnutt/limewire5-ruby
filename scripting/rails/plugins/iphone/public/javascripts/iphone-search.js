jQuery(document).ready(function(){
  var submitSearch = function() {

  var startDownload = function(ev) {
    li = $(ev.target).parent();
    li.addClass("downloading");
    urn = li.attr('id');
    guid = $("#results input[name=guid]").val();
    $.post("/downloads", { urn: urn, guid: guid });
    var done = false;
    $.periodic(function(controller) {
      if(done) {
	li.click(function() {
	  window.location = "/library/"+urn+".mp3";
	  return false;
	});
	controller.stop();
      }
      $.getJSON("/downloads/"+urn, function(download) {
	li.find('.percent').text(download.percent_complete);
		  console.log(download.percent_complete);
	li.find('.progress').css({ width: download.percent_complete+"%" });
	if(download.percent_complete == 100) { done = true; }
      });
      return true;
    }, {frequency: 0.3});
    return false;
  };

    $.getJSON('/search/q/' + $('#q').val(), function(search) {
      guid = search.guid;
      $('#results').append("<input type='hidden' name='guid' value='"+guid+"'>");
      times_refreshed = 0;
      _loadingSearch = false;
      $.periodic(function(controller) {
	if(_loadingSearch) { _loadingSearch = false; return true; }
        if(times_refreshed && times_refreshed > 20) { controller.stop(); }

	_loadingSearch = true;
	times_refreshed ? times_refreshed++ : times_refreshed = 1;

	$.getJSON("/search/" + guid + "/results", function(realData) {
	  results = eval(realData).results;
	  if(results.length > 0) { $("#loading").hide(); }

	  $.each(results, function(index, result) {
	    if($('#'+result.sha1).length == 0) {
	      item =  "<li class='result' id="+result.sha1+">";
	      item += "  <div class='filename'>"+result.filename+"</div>";
	      item += "  <div class='percent'></div>";
	      item += "  <div class='progress'></div>";
	      item += "</li>";
	      $('#results').append(item);
	      $('#results li:last').click(startDownload);
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

  $('#searchForm').submit(submitSearch);
  $('#search_submit').click(submitSearch);
			 $('#results li');
});
