
function sent_browser(margin, width, height, tag) {
	this.margin = margin;
    this.width = width - margin.left - margin.right;
    this.height = height - margin.top - margin.bottom;
    this.tag = tag;
    this.svg = d3.select(tag)
		.append("svg")
		.attr("width", width)
		.attr("height", height)
		.append("g")
		.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
    
    this.box_height = 22;
    this.box_width = width; 
    this.init();
}

sent_browser.prototype = {
	init : function() {
		var self = this;
		self.tdata = [];
		self.height = Math.max(500, self.tdata.length * self.box_height + self.margin.top);
	    d3.select(self.tag).style("height", self.height + "px");
	},
	update : function() {
		var self = this;	
		self.svg.selectAll("g.node").remove();
		
		self.tdata = [];
		for (var i = 0; i < main_sents.length; i++) {
			var tokens = main_sents[i].tokens;
			var bar_text = "S" + i + ":  " + tokens.slice(0, 3).join(" ") + " ... ";
			var num_qa = my_annotator.num_slots[i];
			if (num_qa > 0) {
				bar_text += "(" + num_qa + ")";
			}
			self.tdata.push( {
				text : bar_text,
				sent_id : i,
			});
		}
		
		var nodeEnter = self.svg.selectAll("g.node")
				.data(self.tdata)
				.enter()
				.append("g")
				.attr("class", "node")
				.attr("transform", function(d, i) {
					var xx = 0;
					var yy = self.box_height * i + self.margin.top;
					return "translate(" + xx + "," + yy + ")";
				});
		
		nodeEnter.append("rect")
			.attr("height", self.box_height)
			.attr("width", self.box_width)
			.attr("fill", "#66CCFF")
			.attr("opacity", function(d, i) {
				return my_annotator.sent_id == d.sent_id ? 0.4 : 1e-6;
			})
			.on("click", function(d, i) {
				my_annotator.update(d.sent_id);
				self.update();
			});
		
		nodeEnter.append("text")
			.text(function(d) {
				return d.text;
			})
			.attr("x", 6)
			.attr("y", 14);
	}	
};