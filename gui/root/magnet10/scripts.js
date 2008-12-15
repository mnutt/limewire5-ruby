<!--// hide from old browsers
	var whitespace = " \t\n\r";
	var digitsInCC1 = 13;
	var digitsInCC2 = 15;
	var digitsInCC3 = 16;
	var digitsInEXP = 4;
	
 
   function rf_isEmpty(str) {
    return ((str == null) || (str.length == 0));
   }

	 function rf_isDigit(chr) {
	 return ((chr >= "0") && (chr <= "9"));
   }
 
   
	function rf_isInteger(str) {
	var i;
	if (rf_isEmpty(str))
		return false;
	for (i = 0; i < str.length; i++) {
		var chr = str.charAt(i);
		if (!rf_isDigit(chr))
			return false;
	}
	return true;
  }
   
     function rf_isCC(str) {
	 if (rf_isEmpty(str)) 
	 return false;
	 return (rf_isInteger(str) && ((str.length == digitsInCC1) || (str.length == digitsInCC2) || (str.length == digitsInCC3)));
   }

     function rf_isEXP(str) {
	 if (rf_isEmpty(str)) 
	 return false;
	 return (rf_isInteger(str) && ((str.length == digitsInEXP)));
   }

   function rf_isWhitespace(str) {
    var i;
    if (rf_isEmpty(str))
     return true;
    for (i = 0; i < str.length; i++) {
     var chr = str.charAt(i);
     if (whitespace.indexOf(chr) == -1)
      return false;
    }
    return true;
   }
   
   /////////////////////


	
	////////////////

	function Validator(theForm)	{
   
     if (rf_isWhitespace (document.forms[0].to_email.value))
   {
           alert("You didn't enter where you'd like the links sent")
           theForm.to_email.focus();
           return (false);
   }
  
    // check the EMAIL format
	if (!isEmail(document.forms[0].to_email))
	{
		alert("Please verify that the receiver's email address is in the proper format - name@company.com.");
		theForm.to_email.focus();
		return (false);
	}
  
 
    // check the EMAIL format
	if (!isEmail(document.forms[0].cc_email)&& !rf_isWhitespace (document.forms[0].cc_email.value))
	{
		alert("Please verify that the cc email address is in the proper format - name@company.com.");
		theForm.cc_email.focus();
		return (false);
	}
	
	     if (rf_isWhitespace (document.forms[0].from_email.value))
   {
            alert("You didn't enter your email address.")
           theForm.from_email.focus();
           return (false);
   }
  
    // check the EMAIL format
	if (!isEmail(document.forms[0].from_email))
	{
		alert("Please verify that your email address is in the proper format - name@company.com.");
		theForm.from_email.focus();
		return (false);
	}

  
}


function isEmail(frmField)
{   
	var str = frmField.value;
	if (str.indexOf ('@',0) == -1 || str.indexOf ('.',0) == -1)
		return false;      
	else
		return true;      
}



//-->