
function annotator(margin, width, height, tag) {
	this.margin = margin;
	this.width = width - margin.left - margin.right;
    this.height = height - margin.top - margin.bottom;
    this.svg = d3.select(tag)
		.append("svg")
		.attr("width", width)
		.attr("height", height)
		.append("g")
		.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
    this.init();
}

annotator.prototype = {
	init : function() {
		this.sent_id = 0;
	},
	clear : function() {
		this.svg.selectAll("text").remove();
	},
	update : function() {
		var self = this;
		self.clear();
		
		var tokens = main_sents[self.sent_id].tokens;
		var tdata = [tokens.join(" "),];	
		var sizes = [];
		console.log(tdata);
		
		self.svg
			.selectAll("text")
			.data(tdata)
			.enter()
			.append("text")
			.text(function(d) {
				return d;
			})
			.attr("text-anchor", "left")
			.style("font-size", "14px")
			.style("fill-opacity", 1);
		
		//my_progress.update();
	},
	jump : function(new_sent_id) {
		//this.getAnnotation();
		this.sent_id = new_sent_id;
		this.update();
		//this.setAnnotation();
		my_sent_browser.update();
	},
};