use async_recursion::async_recursion;
use futures::join;
use html2text;
use itertools::Itertools;
use rand::{seq::IteratorRandom, SeedableRng};
use regex::Regex;
use reqwest::*;
use std::collections::{HashMap, HashSet};
use std::io::{Result, Write};
use std::writeln;
use std::{fs, fs::File};
#[macro_use]
extern crate lazy_static;

const LINK: &str = "https://en.wikipedia.org/wiki/";

lazy_static! {
    static ref re: Regex = Regex::new(r#""/wiki/([[:word:]]+)""#).unwrap();
}

#[async_recursion]
async fn initial_crawl(article_title: String, remaining_iter: usize) -> Vec<(String, Vec<String>)> {
    let body = get(format!("{}{}", LINK, article_title))
        .await
        .unwrap()
        .text()
        .await
        .unwrap();

    let capture_strings: Vec<_> = re
        .captures_iter(&body)
        .map(|capture| capture.get(1).unwrap().as_str().to_string())
        .collect();

    let mut rng = rand::rngs::StdRng::seed_from_u64(42);

    let next_articles = capture_strings.iter().cloned().choose_multiple(&mut rng, 2);
    let current_result = vec![(article_title, capture_strings)].into_iter();
    if remaining_iter > 0 {
        let results = join!(
            initial_crawl(next_articles[0].to_string(), (remaining_iter - 1) / 2),
            initial_crawl(next_articles[1].to_string(), (remaining_iter - 1) / 2),
        );
        current_result
            .chain(results.0.into_iter())
            .chain(results.1.into_iter())
            .collect()
    } else {
        current_result.collect()
    }
}

async fn crawl(initial_article: String, iter_number: usize) -> Result<String> {
    let mut links_hash_map: HashMap<String, Vec<String>> = initial_crawl(initial_article, iter_number - 1)
        .await
        .into_iter()
        .collect();
    let hashmap_keys: HashSet<_> = links_hash_map.keys().cloned().collect();
    links_hash_map.iter_mut().for_each(|(key, strings)| {
        strings.retain(|string| hashmap_keys.contains(string) && !(&string).eq(&key));
        strings.sort_unstable();
        strings.dedup();
    });
    let filename = format!("./crawl_data.txt",);
    let mut file = File::create(&filename)?;
    for (key, values) in links_hash_map {
        for value in values {
            writeln!(file, "{} {}", key, value);
        }
    }
    Ok(filename)
}
fn create_hash_map_from_file_content(contents: &str) -> HashMap<&str, Vec<&str>> {
    contents
        .lines()
        .into_iter()
        .map(
            |line| match line.split_whitespace().collect::<Vec<_>>()[..] {
                [a, b] => [a, b],
                _ => panic!("Elements in row don't equal 2"),
            },
        )
        .fold(
            HashMap::new(),
            |mut acc: HashMap<&str, Vec<&str>>, [key, value]| {
                acc.entry(key).or_default().push(value);
                acc
            },
        )
}
fn page_rank(filename: String, iters_num: usize) -> Vec<String> {
    let contents = fs::read_to_string(filename).expect("Something went wrong reading the file");

    let links_hash_map = create_hash_map_from_file_content(&contents);
    let mut ranks: HashMap<&str, f64> = links_hash_map
        .to_owned()
        .into_iter()
        .map(|(k, _)| (k, 1.0))
        .collect();

    for _ in 0..iters_num {
        let contribs = links_hash_map
            .iter()
            .map(|(key, value)| {
                (
                    value,
                    match ranks.get(key) {
                        Some(value) => *value,
                        None => 0.0,
                    },
                )
            })
            .flat_map(|(url_vec, rank)| {
                let size = url_vec.len();
                url_vec
                    .iter()
                    .map(move |url| (url, rank / size as f64))
                    .collect::<Vec<_>>()
            })
            .collect::<Vec<_>>();

        ranks = contribs
            .into_iter()
            .fold(
                HashMap::new(),
                |mut acc: HashMap<&str, f64>, (key, value)| {
                    let count = acc.entry(key).or_default();
                    *count += value;
                    acc
                },
            )
            .into_iter()
            .map(|(key, value)| (key, 0.15 + 0.85 * value))
            .collect();
    }
    println!("{:#?}", ranks);
    // assert_eq!(dbg!(ranks.keys().len()),hashmap.keys().len());
    ranks
        .into_iter()
        .sorted_by(|left, right| (&right.1).partial_cmp(&left.1).unwrap())
        .map(|(k, _)| k.to_string())
        .collect()
}

fn analyze_article(filename: String, analyzed_article: String) {
    let contents = fs::read_to_string(filename).expect("Something went wrong reading the file");

    let links_map = create_hash_map_from_file_content(&contents);

    let inv_links_map = links_map.to_owned().into_iter().fold(
        HashMap::new(),
        |mut acc: HashMap<&str, Vec<&str>>, (key, values)| {
            values.iter().for_each(|value| {
                acc.entry(value).or_default().push(key);
            });
            acc
        },
    );
    inv_links_map.get(analyzed_article.as_str());
}

async fn search_engine(sorted_links: Vec<String>)-> HashMap<String,Vec<String>> {

    let word_regex: Regex = Regex::new(r#"\w+"#).unwrap();
    let mut words_hash_map: HashMap<String,Vec<String>> = HashMap::new();
    for link in sorted_links {
        let body = get(format!("{}{}", LINK, link))
            .await
            .unwrap()
            .text()
            .await
            .unwrap();
        println!("3");
        let mut words: Vec<_> = word_regex
            .captures_iter(&body)
            .map(|c| c.get(0).unwrap().as_str().to_string()).sorted_unstable()
            .collect();
        words.dedup();
        for word in words {
            words_hash_map.entry(word).or_default().push(link.to_owned())
        }
    }
    words_hash_map
}
#[tokio::main]
async fn main() -> Result<()> {
    let initial_article = "Jesus".to_string();
    let crawl_iter_number = 512;
    let filename = dbg!(crawl(initial_article, crawl_iter_number).await.unwrap());
    let page_rank_iter_number = 100;
    let sorted_links = page_rank(filename, page_rank_iter_number);
    
    let words_hash_map = search_engine(sorted_links).await;
    println!("{:#?}", words_hash_map["potential"]);
    Ok(())
}
