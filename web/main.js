
var main_sents = [
                  /*
    {
    	tokens: ["This", "is", "a", "test", "for", "constituents", "."], 
    	phrases : [
    	    { left: 1, right: 6, labels: ["VP"], in_gold : 1, in_pred : 1},
    	    { left: 2, right: 4, labels: ["NP"], in_gold : 1, in_pred : 1},
    	    { left: 2, right: 6, labels: ["NP"], in_gold : 1, in_pred : 1},
    	    { left: 4, right: 6, labels: ["PP"], in_gold : 1, in_pred : 1},
    	]
    }, */
];

var main_qlist = []; // a list of phrase ids for each sentence, where the phrase contains a question

var max_num_qs = 1;
var main_task = "question";

$("input, select").keydown(function(e) {
    if (e.keyCode == 40 || e.keyCode == 13) {
        $(this).next('input, select').focus();
    } else if (e.keyCode == 38) {
    	$(this).prev('input, select').focus();
    }
});

var my_sent_browser = new sent_browser(
		{ top:10, right:10, bottom:10, left:10}, 300, 600, "#browser");

var my_annotator = new annotator(
		{ top:10, right:10, bottom:10, left:10},  1200, 50, "#annotator");

function load_from() {
	var filename = $("#filepath_input").val();
	d3.json("./data/" + filename, function(data) {
		main_sents = data["sentences"];
		//init_data();
		//init_annotator();
		console.log("Loaded " + main_sents.length + " sentences.");
		my_annotator.init();
		my_annotator.update();
		my_sent_browser.init();
		my_sent_browser.update();
	});
}
