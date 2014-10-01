
var main_sents = [];

$(".annotation_input").keydown(function(e) {
    if (e.keyCode == 40 || e.keyCode == 13) {
        $(this).next(".annotation_input").focus();
    } else if (e.keyCode == 38) {
    	$(this).prev(".annotation_input").focus();
    }
});

var my_sent_browser = new sent_browser(
		{ top:10, right:10, bottom:10, left:10}, 300, 600, "#browser");

var my_annotator = new annotator("#annotator");

function load_from() {
	var filename = $("#filepath_input").val();
	d3.json("./data/" + filename, function(data) {
		main_sents = data["sentences"];
		console.log("Loaded " + main_sents.length + " sentences.");
		
		my_annotator.init();
		my_annotator.update(0);
		my_sent_browser.init();
		my_sent_browser.update();
	});
}

function save_as() {
	my_annotator.get_annotation();
	var results_data = "text/json;charset=utf-8," + encodeURIComponent(
			JSON.stringify(main_sents, null, '\t'));
	var filename = $("#filepath_output").val();
	d3.selectAll("a").remove();
	$('<a href="data:' + results_data + '" download="' + filename + '"> download JSON file </a>').appendTo("body");
}

// source: http://stackoverflow.com/questions/3148195/jquery-ui-autocomplete-use-startswith
// Overrides the default autocomplete filter function to search only from the beginning of the string
$.ui.autocomplete.filter = function (array, term) {
    var matcher = new RegExp("^" + $.ui.autocomplete.escapeRegex(term), "i");
    return $.grep(array, function (value) {
        return matcher.test(value.label || value.value || value);
    });
};

