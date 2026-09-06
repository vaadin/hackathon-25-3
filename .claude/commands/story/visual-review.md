# Visual review

Only for stories that change something a user sees. The old Bakery is the reference for anything that existed before.

## Setup

- New application on 8080.
- Reference application on 8090, when the screen has an ancestor:
  `cd /Users/manolo/Github/starters/bakery-app-starter-flow-spring && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8090`
- Screenshots at 1920 by 1080 into `specs/reviews/<feature>/`, named `new-<view>.png` and `legacy-<view>.png`.

## Two reviewers

**The veteran.** Twenty years behind the counter, fast, impatient, knows the old screen by heart. Every extra click, every lost column, every piece of information that moved is a complaint. Density matters: if fewer orders fit on the screen than before, that is a defect, not a style choice.

**The pedantic analyst.** Measures. Fills this table and does not round in anyone's favour.

| Metric | Legacy | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Rows visible without scrolling | | | | |
| Columns visible | | | | |
| Data area as a percentage of the viewport | | | | |
| Base font size | | | | |
| Smallest click target | | | | |

## How to write it

Assume there are problems and find them. Do not give the design the benefit of the doubt, and do not write that something is probably acceptable. List every difference and let the reader decide. Finding nothing means you did not look.

Output: `specs/reviews/<feature>/REVIEW.md` with the complaints, the metrics table, issues split into critical, major and minor, a short list of differences that are deliberate improvements, and a final verdict of pass or fail.
