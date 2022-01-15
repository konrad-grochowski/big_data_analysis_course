use async_recursion::async_recursion;

use futures::join;
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
    let mut hashmap: HashMap<String, Vec<String>> = initial_crawl(initial_article, iter_number - 1)
        .await
        .into_iter()
        .collect();
    let hashmap_keys: HashSet<_> = hashmap.keys().cloned().collect();
    hashmap.iter_mut().for_each(|(key, strings)| {
        strings.retain(|string| hashmap_keys.contains(string) && !(&string).eq(&key));
        strings.sort_unstable();
        strings.dedup();
    });
    let filename = format!("./crawl_data.txt",);
    let mut file = File::create(&filename)?;
    for (key, values) in hashmap {
        for value in values {
            writeln!(file, "{} {}", key, value);
        }
    }
    Ok(filename)
}
fn create_hashmap_from_file_content(contents: &str) -> HashMap<&str, Vec<&str>> {
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
fn page_rank(filename: String, iters_num: usize) -> String {
    let contents = fs::read_to_string(filename).expect("Something went wrong reading the file");

    let hashmap = create_hashmap_from_file_content(&contents);
    let mut ranks: HashMap<&str, f64> = hashmap
        .to_owned()
        .into_iter()
        .map(|(k, _)| (k, 1.0))
        .collect();

    for _ in 0..iters_num {
        let contribs = hashmap
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
    "a".into()
}

fn analyze_article(filename: String, analyzed_article: String) {
    let contents = fs::read_to_string(filename).expect("Something went wrong reading the file");

    let links_map = create_hashmap_from_file_content(&contents);

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
#[tokio::main]
async fn main() -> Result<()> {
    let initial_article = "Asia".to_string();
    let crawl_iter_number = 5;
    let filename = dbg!(crawl(initial_article, crawl_iter_number).await.unwrap());
    let page_rank_iter_number = 100;
    let _x = page_rank(filename, page_rank_iter_number);
    Ok(())
}
