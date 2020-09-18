package com.future.hackday.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GoogleSearchClient {

    private static final String GOOGLEAPIS_CUSTOMSEARCH_URI = "https://www.googleapis.com/customsearch/";
    private static final String GOOGLE_APIS_CUSTOMSEARCH_PATH = "v1";
    private static final String GOOGLE_SEARCH_API_KEY = "AIzaSyB1CNI4bJsDo_19R5iz5aXGbqFfJJT-Oy8";
    private static final String GOOGLE_SEARCH_API_CX = "b2365c75b8fb3486e";

    private static final int MAX_WORDS_OCCURRING_OFTEN_TO_SEARCH = 10;
    private static final int MAX_PROPER_NOUNS_TO_SEARCH = 10;
    private static final int MAX_PHRASES_TO_SEARCH = 8;

    private final Executor executor = Executor.newInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleSearchClient() {}

    public Map<String, Double> searchOn(final String aggregateSearchString) {
        try {
            final List<String> multipleSearchStrings = divideIntoBetterSearchStrings(aggregateSearchString);
            final List<List<String>> totalUrls = new ArrayList<>();
            for (String searchString : multipleSearchStrings) {
                if (!searchString.isEmpty()) {
                    final URI requestUri = new URIBuilder(URI.create(GOOGLEAPIS_CUSTOMSEARCH_URI).resolve(GOOGLE_APIS_CUSTOMSEARCH_PATH))
                            .addParameter("key", GOOGLE_SEARCH_API_KEY)
                            .addParameter("cx", GOOGLE_SEARCH_API_CX)
                            .addParameter("q", searchString)
                            .build();
                    final Request request = Request.Get(requestUri);
                    final HttpResponse response = executor.execute(request).returnResponse();
                    @SuppressWarnings("unchecked")
                    final Map<String, List<Map<String, String>>> retMap = ((ResponseHandler<Map>) response1 -> {
                        final int statusCode = response1.getStatusLine().getStatusCode();
                        if (statusCode >= 300) {
                            throw new IllegalStateException(String.valueOf(statusCode));
                        }
                        return objectMapper.readValue(response1.getEntity().getContent(), Map.class);
                    }).handleResponse(response);
                    List<String> urls = new ArrayList<>();
                    final List<Map<String, String>> items = retMap.get("items");
                    if (items != null) {
                        for (Map<String, String> item : items) {
                            final String url = item.get("link");
                            urls.add(url);
                        }
                    }
                    totalUrls.add(urls);
                }
            }
            return findBestUrls(totalUrls);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            return ImmutableMap.of();
        }
    }

    private List<String> divideIntoBetterSearchStrings(String incoming) {
        //
        // Return top 8 "phrases" bordered by period, comma, semi-colon, colon, or underscore,
        // then return the top 10 words found and put those into *one* string, then return
        // the top 10 "proper nouns" (words beginning with a capital letter but not after
        // a period) and put those into *one* string.
        //
        // So, there will be a total of up to 10 strings returned.
        //

        final List<String> searchStrings = new ArrayList<>();

        final List<String> phrases = addLongestPhrases(incoming, searchStrings);

        addWordsOccurringMostOften(phrases, searchStrings);

        addProperNouns(incoming, searchStrings);

        return searchStrings;

    }

    private List<String> addLongestPhrases(String incoming, List<String> searchStrings) {
        final List<String> phrases = Arrays.asList(incoming.split("[.,;:_]"));

        final List<String> phrasesSortedByLength = phrases.stream().sorted(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o2.length() > o1.length()) {
                    return 1;
                } else if (o1.length() > o2.length()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }).collect(Collectors.toList());

        searchStrings.addAll(phrasesSortedByLength.size() > MAX_PHRASES_TO_SEARCH ? phrasesSortedByLength.subList(0, MAX_PHRASES_TO_SEARCH) : phrasesSortedByLength);
        return phrases;
    }

    private void addWordsOccurringMostOften(List<String> phrases, List<String> searchStrings) {
        final List<String> words = new ArrayList<>();

        for (String phrase : phrases) {
            words.addAll(Arrays.asList(phrase.split("\\s")));
        }

        final Map<String, Integer> wordMap = new HashMap<>();

        for (String word : words) {
            final Integer tokenCount = wordMap.get(word);
            if (tokenCount == null) {
                wordMap.put(word, 1);
            } else {
                wordMap.remove(word);
                wordMap.put(word, tokenCount+1);
            }
        }

        final SortedSetMultimap<Float, String> sortedWordMap = TreeMultimap.create(Comparator.reverseOrder(), // Key sorting
                                                                                   Comparator.naturalOrder()  // Value sorting
        );

        wordMap.entrySet().forEach(e -> sortedWordMap.put(Float.valueOf(e.getValue()), e.getKey()));

        StringBuilder wordsOccurringMostOften = new StringBuilder();
        int count = 0;
        for (String value : sortedWordMap.values()) {
            wordsOccurringMostOften.append(value).append(" ");
            count++;
            if (count >= MAX_WORDS_OCCURRING_OFTEN_TO_SEARCH) {
                break;
            }
        }
        if (wordsOccurringMostOften.length() > 0) {
            wordsOccurringMostOften.deleteCharAt(wordsOccurringMostOften.length() - 1);
        }

        searchStrings.add(wordsOccurringMostOften.toString());
    }

    private void addProperNouns(String incoming, List<String> searchStrings) {
        StringBuilder properNouns = new StringBuilder();
        final String regex = ".\\s+[A-Z][a-z0-9_]+";
        //Creating a pattern object
        final Pattern pattern = Pattern.compile(regex);
        //Matching the compiled pattern in the String
        Matcher matcher = pattern.matcher(incoming);
        int count = 0;
        while (matcher.find() && count < MAX_PROPER_NOUNS_TO_SEARCH) {
            final String matchedString = matcher.group();
            if (matchedString.charAt(0) != '.') {  // ignore capitalization after end-of-sentence
                properNouns.append(matcher.group().substring(1).replaceAll("\\s", "")).append(" ");
                count++;
            }
        }
        if (properNouns.length() > 0) {
            properNouns.deleteCharAt(properNouns.length() - 1);
        }
        searchStrings.add(properNouns.toString());
    }

    private SortedMap<String, Double> findBestUrls(List<List<String>> totalUrls) {  // Urls earlier in list are given higher priority but recurring URLs get bonus points
        int numTotalUrls = 0;
        for (List<String> urlList : totalUrls) {
            numTotalUrls += urlList.size();
        }

        int count = 0;
        int count2 = 0;
        SortedMap<String, Double> prelimMap = new TreeMap<>();
        for (List<String> urls : totalUrls) {
            for (String url : urls) {
                final Double score = prelimMap.get(url);
                if (score == null) {
                    prelimMap.put(url, 1000.0 - (((count2*urls.size())/numTotalUrls)+count)*100.0);
                } else {
                    prelimMap.put(url, score * 1.1);
                }
                count2++;
            }
            count++;
        }

        return prelimMap;
    }
}
