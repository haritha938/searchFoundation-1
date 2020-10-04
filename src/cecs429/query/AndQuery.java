package cecs429.query;

import cecs429.index.Index;
import cecs429.index.Posting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An AndQuery composes other Query objects and merges their postings in an intersection-like operation.
 */
public class AndQuery implements Query {

	private List<Query> mChildren;
	private boolean isNegativeQuery=false;
	
	public AndQuery(Iterable<Query> children) {
		mChildren = new ArrayList<>((Collection<? extends Query>) children);
	}
	
	@Override
	public List<Posting> getPostings(Index index) {
		List<Posting> result = new ArrayList<>();
		
		// TODO: program the merge for an AndQuery, by gathering the postings of the composed QueryComponents and
		// intersecting the resulting postings.
		int ChildernCount=mChildren.size();
		if(ChildernCount==1)
		{
			result= mChildren.get(0).getPostings(index);

		}
		else
		{
			List<Posting> postings=new ArrayList<>();
			List<Posting> ResultantPostings=new ArrayList<>();
			List<Query> NotQueries=new ArrayList<Query>();
			boolean isFirstPositiveQuery=true;
			for(int i=0;i<ChildernCount;i++)
			{
				if(mChildren.get(i).IsNegativeQuery())
				{
					Query q=new NotQuery(mChildren.get(i));
					NotQueries.add(q);
					continue;
				}
				if(isFirstPositiveQuery)
				{
					ResultantPostings=mChildren.get(i).getPostings(index);
					isFirstPositiveQuery=false;
					continue;
				}

				postings=mChildren.get(i).getPostings(index);
				if(postings!=null)
				{
					ResultantPostings=AndMerge(ResultantPostings,postings);
				}
				else
					return null;

			}
			
			if((NotQueries != null ? NotQueries.size() : 0) >0)
			{
				if(ResultantPostings!=null)
				{
					ResultantPostings=AndNotMerge(ResultantPostings,NotQueries,NotQueries.size()-1, index);
				}
				else
				{
					System.out.println("No And query..Only not queries are available. Hence not a valid query");
				}
			}

			result=ResultantPostings;
		}

		return result;
	}

	@Override
	public boolean IsNegativeQuery() {
		return false;
	}


    public  List<Posting> AndNotMerge(List<Posting> PosTermPosting, List<Query> NotQuery, int index,Index CorpusIndex)
	{
		if(index<0)
		{
			return PosTermPosting;
		}
		else {
			int i = 0, j = 0;
			List<Posting> NegativeTermpostings=NotQuery.get(index).getPostings(CorpusIndex);
			int notTermPostingSize=NegativeTermpostings.size();
			int resultPostingSize=PosTermPosting.size();
			List<Posting> Result=new ArrayList<>();
			while (i<resultPostingSize && j<notTermPostingSize)
			{
				if(NegativeTermpostings.get(j).getDocumentId()==PosTermPosting.get(i).getDocumentId())
				{

					i++;
					j++;
				}
				else if(PosTermPosting.get(i).getDocumentId()<NegativeTermpostings.get(j).getDocumentId())
				{
					Result.add(PosTermPosting.get(i));
					i++;
				}
				else //PosTermPosting.get(i).getDocumentId()>NegativeTermpostings.get(j).getDocumentId()
				{
					j++;
				}
			}
			while(i<resultPostingSize)
			{
				Result.add(PosTermPosting.get(i));
				i++;
			}



			return AndNotMerge(Result,NotQuery,index-1,CorpusIndex);
		}
	}
	public List<Posting> AndMerge(List<Posting> A,List<Posting> B)
	{
		List<Posting> AndMergeResult = new ArrayList<>();
		int aDocId = 0, bDocId = 0;
		int aSize = A.size();
		int bSize = B.size();
		int i = 0, j = 0;
		while (i < aSize && j < bSize) {

			aDocId = A.get(i).getDocumentId();
			bDocId = B.get(j).getDocumentId();

			if (aDocId == bDocId) {
				AndMergeResult.add(A.get(i));
				i++;
				j++;
			} else if (aDocId < bDocId) {
				i++;
			} else //aDocId > bDocId
			{
				j++;
			}

		}

		return AndMergeResult;
	}
	
	@Override
	public String toString() {

		return
		 String.join(" ", mChildren.stream().map(c -> c.toString()).collect(Collectors.toList()));
	}
}
