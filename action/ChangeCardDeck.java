package action;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;

import walker.ErrorData;
import walker.Info;
import walker.Process;

public class ChangeCardDeck {

	private static final String URL_CHANGE_CARD_DECK = "http://web.million-arthurs.com/connect/app/cardselect/savedeckcard?cyt=1";
	private static byte[] response;

	public static boolean run() throws Exception {

		if (GetCardDeck.run()) {
			if (Info.LastDeck.card.equals(Process.info.CurrentDeck.card))
				return true;
		} else {
			walker.Go.log("Something wrong@GET_CARD_DECK.", !Info.Nolog);
			return false;
		}

		ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
		post.add(new BasicNameValuePair("C", Process.info.CurrentDeck.card));
		int cards = 0;
		for (String i : Process.info.CurrentDeck.card.split(",")) {
			if (!i.equals("empty")) {
				cards++;
			}
		}
		if (cards != 2) {
			post.add(new BasicNameValuePair("lr",
					Process.info.CurrentDeck.leader));
		}
		post.add(new BasicNameValuePair("no", "2"));
		try {
			response = walker.Process.network.ConnectToServer(
					URL_CHANGE_CARD_DECK, post, false);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.text;
			ErrorData.currentErrorType = ErrorData.ErrorType.ConnectionError;
			ErrorData.text = ex.getMessage();
			throw ex;
		}

		// Thread.sleep(Process.getRandom(1000, 2000));

		if (Info.Debug) {
			String clazzName = new Object() {
				public String getClassName() {
					String clazzName = this.getClass().getName();
					return clazzName.substring(0, clazzName.lastIndexOf('$'));
				}
			}.getClassName();
			walker.Go.saveXMLFile(response, clazzName);
		}

		Document doc;
		try {
			doc = Process.ParseXMLBytes(response);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.ChangeCardDeckDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		try {
			return parse(doc);
		} catch (Exception ex) {
			throw ex;
		}
	}

	private static boolean parse(Document doc) throws Exception {

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();

		try {
			if (!xpath.evaluate("/response/header/error/code", doc).equals("0")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.ChangeCardDeckResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}
			if (!xpath.evaluate("/response/body/save_deck_card/result", doc)
					.equals("0"))
				return false;

			Info.MyDeck0.card = xpath.evaluate(
					"//deck[deckname='デッキ1']/deck_cards", doc);
			Info.MyDeck1.card = xpath.evaluate(
					"//deck[deckname='デッキ2']/deck_cards", doc);
			Info.MyDeck2.card = xpath.evaluate(
					"//deck[deckname='デッキ3']/deck_cards", doc);

			walker.Go.saveDeck(0, Info.MyDeck0.card);
			walker.Go.saveDeck(1, Info.MyDeck1.card);
			walker.Go.saveDeck(2, Info.MyDeck2.card);

			if (!Info.MyDeck2.card.equals(Process.info.CurrentDeck.card))
				return false;

		} catch (Exception ex) {
			if (ErrorData.currentErrorType != ErrorData.ErrorType.none)
				throw ex;
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.ChangeCardDeckDataParseError;
			ErrorData.bytes = response;
			throw ex;
		}

		return true;
	}
}