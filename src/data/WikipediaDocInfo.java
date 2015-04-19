package data;

public class WikipediaDocInfo {
	public int docId;
	public String url, title;
	public int[] sentIdSpan;
	
	public WikipediaDocInfo(String docInfo) {
		// Parse Wikipedia doc info, i.e.
		// <doc id="358" url="http://en.wikipedia.org/wiki?curid=358" title="Algeria">
		String info[] = docInfo.trim().replace("<doc id=\"", "")
				.replace("\" url=\"", "\t")
				.replace("\" title=\"", "\t")
				.replace("\">", "").split("\t");
		docId = Integer.parseInt(info[0]);
		url = info[1];
		title = info[2];
		sentIdSpan = new int[] {-1, -1};
	}
	
	public String toString() {
		return docId + "\t" + url + "\t" + title + "\t" + sentIdSpan[0] +
				"-" + sentIdSpan[1];
	}
	
	public static void main(String[] args) {
		String infoStr = "<doc id=\"358\" url=\"http://en.wikipedia.org/wiki?curid=358\" title=\"Algeria\">";
		
		WikipediaDocInfo wikiInfo = new WikipediaDocInfo(infoStr);
		System.out.println(wikiInfo);
	}
}
