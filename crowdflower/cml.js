require(['jquery-noconflict','bootstrap-modal','bootstrap-tooltip','bootstrap-popover','jquery-cookie'], function($) {
  Window.implement('$', function(el, nc){
    return document.id(el, nc, this.document);
  }); 

  var $ = window.jQuery;
  
  $('.qa-panel').popover({
    html:true,
    placement:'left',
    trigger:'hover',
    title:'Hint',
    content:'<p>Please make sure the questions you asked are grammatical and understandable by others.</p>'
  });
  
  $('.q-panel-label').popover({
    html:true,
    placement:'left',
    trigger:'hover',
    title:'Hint',
    content:'<p>Build a question with the dropdown boxes. Please look at the examples in the instruction if you are not sure.</p>'
  });
  
  /*
  $('.cml-qslot > label').popover({
    html:true,
    placement:'right',
    trigger:'hover',
    title:'Hint',
    content:'<p>Build a question with the dropdown boxes. Please look at the examples in the instruction if you are not sure.</p>'
  });
  */
    
  
  $('.cml-qslot').on('change', function (event) {
    var slot_name = parseSlotName(event.srcElement.name);
    var row_name = slot_name.match(/q[0-9]+/);
    var qstr = getQuestion(row_name);
    $("#show_" + row_name).html("<span>" + qstr + "</span>");
  });
  
  /*
  $('.cml-chk-noq').on('change', function (event) {
    if (event.srcElement.value == "true") {
      $("#show_q0").html("");
      $("#show_a0").html("");
    }
  });
  */
  
  $('.cml-aslot').on('change', function (event) {
   var slot_name = parseSlotName(event.srcElement.name);
   var astr = "";
   $("." + slot_name).each(function() {
     if (astr.length == 0) {
       astr += " - ";
     } else {
       astr += " / ";
     }
      astr += $(this).val();
    });
   //var astr = " - " + $("." + slot_name).val();
   $("#show_" + slot_name).html("<span>" + astr + "</span>");
  });

  function getQuestion(pfx) {
    var qslots = ["wh", "aux", "ph1", "trg", "ph2", "pp", "ph3"];
    var qstr = "";
    for (var i = 0; i < qslots.length; i++) {
      var opt = $("." + pfx + qslots[i] + " option:selected").val();
      if (opt.length > 0) {
        qstr += opt + " ";
      }
    }
    qstr += "?";
    return qstr;
  }

});

function parseSlotName(long_name) {
  var p1 = long_name.indexOf("[") + 1;
  var p2 = long_name.indexOf("]");
  return long_name.substring(p1, p2);
}

// This block if/else block is used to hijack the functionality of an existing validator (specifically: yext_no_international_url)
if(!_cf_cml.digging_gold) {
  CMLFormValidator.addAllThese([
    ['yext_no_international_url', {
      errorMessage: function(){
        return ('Answer is empty/One of your answers contains words that are not in the original sentence.');
      },
      validate: function(element, props) {
        // METHOD_TO_VALIDATE must return true or false
        result = allWordsExistInSentence(element);
        return result[0];
      }
    }]
  ]);
} else {
  CMLFormValidator.addAllThese([
   ['yext_no_international_url', {
      validate: function(element, props) {
         return true;
      }
   }]          
  ]);
}
 
// This is the method that will evaluate your validation
// value is the user submitted content of the form element you are validating
function allWordsExistInSentence(element) {
  var answerWords = element.value.trim().split(" ");
  var sentWords = document.getElementById("s0").firstChild.nodeValue.split(" ");
  for (var i = 0; i < answerWords.length; i++) {
    var found = false;
    for (var j = 0; j < sentWords.length; j++) {
      /*
      var w1 = answerWords[i].toUpperCase();
      var w2 = sentWords[j].toUpperCase();
      console.log(w1);
      console.log(w2);
      for (var k = 0; k < w1.length; k++) {
        console.log(w1.charCodeAt(k) + ", " + w2.charCodeAt(k));
      }
      */
      if (answerWords[i].toUpperCase() === sentWords[j].toUpperCase()) {
        found = true;
        break;
      }
    }
    if (!found) {
      return [false, answerWords[i]];
    }
  }
  return [true, ""];
}

