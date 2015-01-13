// This block if/else block is used to hijack the functionality of an existing validator (specifically: yext_no_international_url)
if(!_cf_cml.digging_gold) {
  CMLFormValidator.addAllThese([
    ['yext_no_international_url', {
      errorMessage: function(){
        return ('One of your answers contains words that are not in the original sentence.');
      },
      validate: function(element, props){
        // METHOD_TO_VALIDATE must return true or false
        result = allWordsExistInSentence(element);
        return result[0];
      }
    }]
  ]);
} else {
  CMLFormValidator.addAllThese([
   ['yext_no_international_url', {
      validate: function(element, props){
         return true;
      }
   }]          
  ]);
}
 
// This is the method that will evaluate your validation
// value is the user submitted content of the form element you are validating
function allWordsExistInSentence(element) {
  var answerWords = element.value.trim().split(" ");
  var sentWords = document.getElementById("s0").innerHTML.split(" ");
  for (var i = 0; i < answerWords.length; i++) {
    var found = false;
    for (var j = 0; j < sentWords.length; j++) {
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