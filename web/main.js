
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

function load_data() {
	$.getJSON( "data/input.json", function(json_data) {
		  main_sents = json_data["sentences"];
		  console.log("Load data: " + main_sents.length + " sentences.")
	});
}

load_data();
