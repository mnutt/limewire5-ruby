document.write(magnetOptionsPreamble);


document.write("<img hspace=10 src=\"http://127.0.0.1:"+(45100+magnetCurrentSlot)+"/magnet10/limewire.gif\">");
document.write("<a href=\"http://127.0.0.1:"+(45100+magnetCurrentSlot)+"/magnet:?"+magnetQueryString+"\">Download this file with LimeWire</a>");
document.write("<br>");
document.write("<font size=-1>");
document.write("MAGNET URI: magnet:?"+magnetQueryString);
document.write("</font>");
document.write(magnetOptionsPostamble);
// at least one option block has displayed
magnetOptionsPollSuccesses++;
