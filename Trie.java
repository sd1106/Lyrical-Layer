import java.util.*;

public class Trie{

	private Node root;
	private List<String[]> lines;

	public Trie(){
		root = new Node();
		lines = new ArrayList<>();
	}

	public void insert(String word){
		if(word == null)
			return;
		Node curr = root;
		curr.passCount++; // passCount here is overall number of words

		for(char c : word.toCharArray()){
			Node child = curr.children.get(c);
			if(child == null){
				//create a new node to hold info about the char
				child = new Node();
				curr.children.put(c, child); //put the child in the map
			}

			curr = child; //move to the newly created child
			curr.passCount++;
		}
		curr.endCount++; //add end of word marker

	}
	public boolean containsPrefix(String prefix)
	{
		if(prefix==null||prefix.length()==0)
			return false;
		Node curr=root;
		for(char c:prefix.toCharArray())
		{
			curr=curr.children.get(c);
			if(curr==null)
				return false;
		}
		return true;
	}
	public boolean contains(String word){
		if(word == null || word.length() == 0){
			return false;
		}

		Node curr = root;

		for(char c : word.toCharArray()){

			Node child = curr.children.get(c);
			if(child == null)
				return false;

			curr = child;
		}

		return curr.isEndOfWord();

	}

	public char mostLikelyNextChar(String prefix){

		if(prefix == null)
			return '_';

		Node curr = root;

		for(int i = 0; i < prefix.length(); i++){
			char c = prefix.charAt(i);
			curr = curr.children.get(c);

			if(curr == null)
				return '_';
		}

		char mostLikely = ' ';
		long highest = -1;

		for(Map.Entry<Character, Node> entry : curr.children.entrySet()){

			Node child = entry.getValue();
			char c = entry.getKey();

			if(child.passCount > highest){
				highest = child.passCount;
				mostLikely = c;
			}

		}

		return mostLikely;

	}

	public String mostLikelyNextWord(String prefix){
		if(prefix == null)
			return "";

		Node curr = root;

		for(int i = 0; i < prefix.length(); i++){
			char c = prefix.charAt(i);
			curr = curr.children.get(c);

			if(curr == null)
				return "";
		}

		StringBuilder sb = new StringBuilder(prefix);

		while(!curr.isEndOfWord() && !curr.children.isEmpty()){
			char bestChar=' ';
			Node best = null;
			for(Map.Entry<Character, Node> entry : curr.children.entrySet()){
				Node child = entry.getValue();
				char c = entry.getKey();

				if(best==null||child.passCount > best.passCount){
					best = child;
					bestChar=c;
				}
			}
			sb.append(bestChar);
			curr=best;
		}
		return sb.toString();
	}

	public void printWordFrequencies()
	{
		printWordFrequencies(root,"");
	}

	private void printWordFrequencies(Node curr, String word)
	{
		if(curr.isEndOfWord())
			System.out.println(word+": "+curr.endCount);
		for(char c:curr.children.keySet())
			printWordFrequencies(curr.children.get(c),word+c);
	}

	public Map<String, Double> mostLikelyNextWords(String prefix)
	{
		Map<String, Double> results = new HashMap<>();
		if (prefix == null)
			return results;

		Node curr = root;

		for (int x = 0; x<prefix.length(); x++)
		{
			curr = curr.children.get(prefix.charAt(x));
			if (curr == null)
				return results;
		}

		List<String> words= new ArrayList<>();
		List<Long> counts=new ArrayList<>();

		collectWords(curr, prefix, words, counts);

		if (words.isEmpty())
			return results;

		for (int x=0;x<counts.size()-1;x++)
		{
			for (int y=x+1; y<counts.size();y++)
			{
				if (counts.get(y)> counts.get(x))
				{
					long tempCount = counts.get(x);
					counts.set(x,counts.get(y));
					counts.set(y,tempCount);

					String tempWord = words.get(x);
					words.set(x,words.get(y));
					words.set(y,tempWord);
				}
			}
		}

		int limit = 5;

		if (words.size() < 5)
			limit = words.size();

		long total = 0;
		for (int i=0; i<limit; i++)
			total +=counts.get(i);

		for (int i=0; i<limit; i++) {
			double p=0.0;
			if (total>0) {
				p = (counts.get(i)*100.0)/total;
				p = Math.round(p* 10) / 10.0;
			}
			results.put(words.get(i),p);
		}
		return results;
	}

	private void collectWords(Node node, String word,List<String> words,List<Long> counts) {

		if (node.isEndOfWord()) {
			words.add(word);
			counts.add(node.endCount);
		}

		for (Map.Entry<Character, Node> entry:node.children.entrySet())
			collectWords(entry.getValue(),word+entry.getKey(),words,counts);
	}

	private String[] guessingGameLine()
	{
		if(lines.isEmpty())
			return new String[0];
		int index=(int)(Math.random()*lines.size());
		return lines.get(index).clone();
	}

	public String[] guessingGame()
	{
		String[] arr=guessingGameLine();
		if(arr.length==0)
			return new String[0];

		int temp=(int)(Math.random()*arr.length);
		String guess = arr[temp];
		arr[temp] = "___";

		String[] arrNew = new String[arr.length+1];

		for(int x=0;x<arr.length;x++)
			arrNew[x] = arr[x];

		arrNew[arrNew.length-1]=guess;
		return arrNew;
	}

	public void insertLine(String[] words)
	{
		if(words == null || words.length == 0)
			return;
		lines.add(words);
		for(String w : words)
			insert(w.toLowerCase());
	}

	/*INNER CLASS*/
	class Node{
		Map<Character, Node> children;
		long passCount;
		long endCount;

		Node(){
			children = new HashMap<Character, Node>();
			passCount = endCount = 0;
		}

		boolean isEndOfWord(){
			return endCount > 0;
		}

		//Mostly for debugging/ testing
		@Override
		public String toString (){
			return "(pass = " + passCount + ", end = " + endCount + ")";
		}
	}

}