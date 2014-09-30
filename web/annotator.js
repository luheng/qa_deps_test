
function annotator(tag) {
	this.tag = tag;
    this.init();
}

annotator.prototype = {
	init : function() {
		this.sent_id = 0;
		this.qa_list = [];
		this.num_slots = [];
		for (var i = 0; i < main_sents.length; i++) {
			this.qa_list.push([]);
			this.num_slots.push(0);
		}
	},
	update : function(new_sent_id) {
		var self = this;
		// Cache annotation.
		self.get_annotation();
		self.sent_id = new_sent_id;
		
		// Update Sentence viewer.
		// TODO: move this to sent_viewer class.
		var tokens = main_sents[self.sent_id].tokens;
		var text = tokens.join(" ");
		$("#p_sentence").remove();
		$("#sentence_viewer").append("<p id=\"p_sentence\">" + text + "</p>");
		
		// Setup auto-completion.
		self.auto_spans = [];
		for (var i = 0; i < tokens.length; i++) {
			for (var j = i + 1; j < tokens.length; j++) {
				self.auto_spans.push(tokens.slice(i, j).join(" "));
			}
		}
		// Update QA slots.
		// TODO: assert number of slots should equal number of qa pairs.
		$(".qa_pair").remove();
		var num_qa = self.num_slots[self.sent_id];
		var cached_qa = self.qa_list[self.sent_id];
		if (num_qa == 0) {
			self.append_qa_slot();
		} else for (var i = 0; i < num_qa; i++) {
			self.draw_qa_slot(i);
			$("#q" + i).val(cached_qa[i][0]);
			$("#a" + i).val(cached_qa[i][1]);
		}
 	},
 	draw_qa_slot : function(slot_id) {
 		var self = this;
 		$(self.tag).append(
				"<div id=\"qa" + slot_id + "\" class=\"qa_pair\">" +
		        "<label for=\"q" + slot_id + "\"> Q" + (slot_id + 1) + ": </label>" +
		        "<input type=\"text\" class=\"annotation_input\" id=\"q" + slot_id + "\" >" +
		        "<br>" +
		        "<label for=\"a" + slot_id + "\"> A" + (slot_id + 1) + ": </label>" +
		        "<input type=\"text\" class=\"annotation_input\" id=\"a" + slot_id + "\" >" +
	            "</div>");
 		$("#a" + slot_id).autocomplete({
			source: self.auto_spans
		});
 	},
	append_qa_slot : function() {
		var self = this;
		var next_slot_id = self.num_slots[self.sent_id];
		self.draw_qa_slot(next_slot_id);
		self.num_slots[self.sent_id] += 1;
	},
	remove_qa_slot : function() {
		var self = this;
		var to_remove = self.num_slots[self.sent_id] - 1; 
		if (to_remove == 0) {
			$("#q0").val("");
			$("#a0").val("");
		} else {
			$("#qa" + to_remove).remove();
			self.num_slots[self.sent_id] -= 1;
		}
		self.get_annotation();
	},
	get_annotation : function() {
		var self = this;
		var curr_num_slots = self.num_slots[self.sent_id];
		var curr_qa = [];
		for (var i = 0; i < curr_num_slots; i++) {
			curr_qa.push([$("#q" + i).val(), $("#a" + i).val()]);
		}
		self.qa_list[self.sent_id] = curr_qa;
	}
};