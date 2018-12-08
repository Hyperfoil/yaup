package perf.yaup.json;

import perf.yaup.StringUtil;
import perf.yaup.xml.pojo.XmlPath;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * WIP
 * Implementation based on http://goessner.net/articles/JsonPath/
 * $.store.book[*].author	the authors of all books in the store
 * $..author	all authors
 * $.store.*	all things in store, which are some books and a red bicycle.
 * $.store..price	the price of everything in the store.
 * $..book[2]	the third book
 * $..book[(@.length-1)]
 * $..book[-1:]	the last book in order.
 * $..book[0,1]
 * $..book[:2]	the first two books
 * $..book[?(@.isbn)]	filter all books with isbn number
 * $..book[?(@.price<10)]	filter all books cheapier than 10
 * $..*	All members of JSON structure.
 */
public class JsonPath {

}
