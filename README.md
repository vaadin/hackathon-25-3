# hackathon-25-2
Now upgraded to 25.2. Immediate issue with Jackson compatibility that was solved by upgrading `spring-boot-starter-parent` to 4.0.4. Tried at first with 4.1.0-RC1, but that gave me some `Cannot access Key[type=org.apache.maven.project.MavenProject, annotation=[none]] outside of a scoping block` error to my `pom.xml`.

Then I decided to try copilot, but had problems with enabling the hotswap agent. First I thought I had it already set up but turns out that was in VS Code, not in my Eclipse. Then I tried downloading three different versions of JetBrains Runtime and two different versions of Hotswap Agent and none of the combinations worked, I kept getting `Unrecognized VM option 'UpdateClasses'`. In the end I turned to my VS Code to figure out where it had saved the working configuration, and when I used that instead it worked. I don't know where I went wrong with the manual installations, beyond that as the very first thing I accidentally downloaded a non-SDK variant of JBR because I didn't spot that those were separate.

When the copilot was finally working I had problems with the copilot's UI elements blocking the top corners of my application, where the nav bar is. I tried adding a new button to the nav bar, but copilot created it within MapView instead and inserted it into the already initialized nav bar after the fact, and although I chose the option to add to the middle, it added it at the very end (where it was hidden by a copilot UI element). And when I asked AI to give the button a click listener that navigates to a new view, it created the new view as a static inner class of MapView and did some horrible hack for navigating back and forth, and when I actually tried the button, copilot started throwing internal errors about MapView being corrupted. So I extracted the new view to a new class myself, renamed it to NothingView, moved it to a correct package, fixed the contents, gave it a better route, added a new button to the nav bar manually, and reverted all changes to MenuView. After that the new view worked, but copilot couldn't recover from the changed route and kept claiming that the old one still exists and yet being unable to find the class when I tried to remove it. Mvn clean cleared that up.

Then I checked back with the recording what the _proper_ way to add a new view was, and tried that instead, but it kept autogenerating an invalid suggestion for the classname (`com\vaadin\example\sightseeing\views.MainView`), and it was rather awkward to change those `\` into dots, because the field didn't allow keyboard navigation (on Windows 11+Chrome). I tried the new icon selection, but as expected, it didn't really do anything in the end because the nav bar in this project is just a bunch of buttons rather than a Menu. And I don't know if that's related to it at all, but actually adding the view got copilot stuck and I had to refresh the page to get it to stop spinning the attempt to add the view. There was a warning triangle next to the spinner, but no popups and nothing in the server log, didn't think to check the browser console until after the fact so it's possible that there would have been something there. The new nav button I added myself because I didn't want to go through the same mess for a second time.

Then I tried to do some random dragging of UI elements into the new SomethingView, and within the view, and changing some 'hugs' to 'fills'. Around this stage I stopped fixing the mess that copilot left into my code for the sake of documenting it (has whitespace done something to offend copilot?). I still had to do several changes manually, because I kept getting compilation errors, because trying to move anything as the last element of any layout that has been initialized earlier seems to be broken and it doesn't move the initialization of the 'moved' element up. Adding as the first element or adding and expanding both work. I also kept needing to restart, because copilot kept complaining about corrupted view quite often even when it didn't manage to cause compilation errors, and it couldn't recover from either gracefully.

I meant to try the annotations too, but ran out of time.

# hackathon-25-1
Now upgraded to 25.1 via manually merging with a new starter project. Removed WebDriverManager and LineAwesome dependencies as unnecessary annoyances. Commented out `@OrderColumn` annotation from `Places.tags` because it caused issues (would have wanted a List instead of a Set, but triggered errors with the old database regardless), although that's not strictly speaking a Vaadin issue. Switched from using a theme to using `@StyleSheet` annotations, although I'm not convinced I did it correctly. Wasn't quite certain which of the new files should have been added to version control so may have excluded more of them than is ideal.

Updated tests to JUnit 6 and ran into the problem that a static import for `Assertions.assertEquals` refuses to work, even if it does work for other assertions and a non-static import works just fine. Also discovered that `vaadin-map-testbench` dependency isn't included by default, so had to add that separately to be able to keep using `MapElement` in the tests. Once the tests finally compiled they passed okay, but each test opened an additional browser window that never closed for some reason that I didn't have time to figure out.

Added some lines on the map with the new `LineStringFeature` (rectangles around the old office and the new office locations) but also ran out of time of adding any tests related to that.

# hackathon-24-7
Now upgraded to 24.7. Also updated osmapi-overpass from 2.0 to 3.0, added separate checks for user data and place data presence., added Grid tooltips with configured positions, added a horrible hack to change Tags View to support multi-selection, and added drag selection to Tags Grid.

Had some problems getting the project state to refresh properly after the initial version update, and the messed-up state kept interfering with the database creation, which was a bit of a blocker. But after multiple cleans, refreshes, vaadin-dances, and mvn installs it finally got the hint and after that my main problems were trying to remember how Flow works in general, and an old issue with line-awesome that I still didn't have time to dig into.

And in all fairness I didn't have time to do all that refreshing within the *previous* hackathon, so there may have been some leftovers from 24.2 still floating around some cache too...

The line-awesome issue is that after login it throws me to [some minified css page](http://localhost:8080/line-awesome/dist/line-awesome/css/line-awesome.min.css?continue) rather than [Map View](http://localhost:8080/). I'm probably doing something wrong with it, but I didn't even remember what it was before I started writing this and I don't have time to look into it any further.

# hackathon-24-6
Now upgraded to 24.6. This required updating Spring Boot version, which in turn caused a conflict with the `selenium.version` property (fixed by upgrading the version), and the H2 database was no longer compatible. Spent ages trying to get the database upgraded (with no success, even H2 Console won't run on my computer), until I had to give up and just re-generate it from scratch. Not ideal, but at least I could move forward.

Once I finally got the server up and running again I could log in, but for some reason that keeps now throwing me to http://localhost:8080/line-awesome/dist/line-awesome/css/line-awesome.min.css?continue instead of the actual application. But at least the application works again, if I navigate there manually.

Also added `org.parttio.line-awesome` dependency and removed the outdated feature flag from the previous round.

Ran out of time (again...) so didn't really get a chance to test the 24.6 features.

# hackathon-24-2
This is the 23.2 hackathon project that was earlier upgraded to 24.1 and now again to 24.2. Meant to try to setup web push notifications, but ran into environment issues and then ran out of time after all the installing and restarting.

## hackathon-24-1
Switched ID types from UUID to Long and added a button for adding a custom-styled marker.

The instructions below belong to the original version. 


## About the APP

### Repository Contents
This is a Vaadin 23.2 App that was downloaded from http://start.vaadin.com

It has 3 views, and there is no menu, so you need to type the URL for each page
  - /map - the map view - needs authentication
  - /places - master-detail for PLACE table - needs admin autentication
  - /tags - master-detail for TAG table - needs admin autentication

__NOTE__: for Hilla 1.2 users there is the [`main-hilla`](https://github.com/vaadin/hackathon-23-2/tree/main-hilla) branch instead.

### Data Generator
When the application starts, data generator creates:
  - Two users for login (user:user and admin:admin)
  - POIs for Turku (places and tags)
     - it uses a the [overpass api](https://wiki.openstreetmap.org/wiki/Overpass_API) querying a public [database](https://overpass-api.de)
     - if you want to fill the database with your city data change [`DataGenerator:CENTER`](https://github.com/vaadin/hackathon-23-2/blob/main/src/main/java/com/vaadin/example/sightseeing/data/generator/DataGenerator.java#L34) and [`DataGenerator:RATIO`](https://github.com/vaadin/hackathon-23-2/blob/main/src/main/java/com/vaadin/example/sightseeing/data/generator/DataGenerator.java#L35)

__NOTE__: Sometimes the query to the public service fails at first becausse a timeout (`OsmBadUserInputException: null`), re-run the app and it should work the second time because the server should have cached the results.

### Running the app

Just run `mvn` and open `https://localhost:8080` in your browser

__NOTE__: It requires JDK 17, but you can use 11 (see tips)

### Database
It uses spring JPA for managing an H2 database that persists in a local file, take a look to the [resources](https://github.com/vaadin/hackathon-23-2/blob/main/src/main/resources/application.properties#L12) file.

H2 has a web console that you can use to check data, just open `http://localhost:8080/h2-console` and type `jdbc:h2:./h2-app` in the URL field, leaving User and Password fields empty.

### Committing your work
Create a branch with your name and push to this repo, eg:

```
git clone https://github.com/vaadin/hackathon-23-2.git
cd hackathon-23-2
mvn
git checkout -b my_named_branch
git commit -m 'My changes'
git push
```

### Showing your work

Write a summary of the main characteristics or your app, it could be in the README.md of your branch, or if you prefer a slide. Screenshots in the summary would be nice.

Optionally you can deploy a live demo in heroku or any accessible server.

You might want to demo your app to the audience at the end of the session.

## The Sightseeing APP

ACME is a Travel Agency that wants to offer their passengers an App that facilitates tourism in cities along the route.

Sightseeing-App works in a way that once users are authenticated, they can access the map and media files.

The Map will show the points of interest around their current position.

Clicking on each place, they can see all the information available for it, and be able to download audio guides, docs, etc.

The app should be usable in small devices.

### Users

No registration page, only ACME admins would be able to register new users

### Places and Tags

Admins can add/remove/modify places in the cities.

Places have a set of tags which are properties describing the POI, and they should be editable.

### Media

Admins can add to each place media files like audio, pictures, documents and videos

### Stats

Optionally there could be a statistic module showing the most visited places, etc.

## Hackathon Challenges

During the Hackathon, you might achieve any of the following challenges

1. Display a marker in the map showing the actual user position, it should be updated when user moves
2. Display markers of all the POIs around the user position
3. Display POI info when Clicking on It
4. Show a clickable list of actions available for each point (urls, media files, etc)
5. Visiting the city works in mobile devices (for admin pages, desktop is enough)
6. Display a button to switch to the admin views
7. Be able to edit Places and their tags as well as disable/enable them
8. Be able to updload media files per place
9. User cannot zoom/move out of the current city

Bonuses
1. App works offline
2. There is a stat module
3. Admin can edit/add POIs by right-clicking on the map
5. Anything you come with, eg. a chat

## Hackaton Goals

The main reason of the hackathon is not the app itself, but be able to play with the new features in Vaadin and give feedback about them.

We will evaluate the following aspects

- Design of the application
- Use cases covered
- Number of new features (v23.2) used in the app
- Number of issues found (if possible reported as tickets)

### New Features in 23.2

Please check the full feature list in the [roadmap page](https://github.com/orgs/vaadin/projects/9), though here you have a summary

- Map (not experimental)
- MenuBar right aligned theme
- MultiSelect Combobox
- TextField pattern
- Vite as default
- License checker offline
- TestBench + Karibu
- Hilla Multimodule
- Autocomplete intelliJ for WC
- Java API for Lumo icons

## Tips

- Compute current position of the user
    You can use this [approach](https://github.com/mstahv/maptesting/blob/main/src/main/java/com/example/application/MainView.java#L44) by [Matti](https://github.com/mstahv/)
- Menu for Admins
    You can combine here 'Java API for Lumo icons' + 'MenuBar right aligned theme'
- Place editor
    You can add the new feature multiselect Combobox for selecting tags    
- You can change the JDK to 11 instead of 17
`mvn versions:set-property -Dproperty=java.version -DnewVersion=11 -q`
- For adjusting POI info returned by overpass service, adjust the [query](https://github.com/vaadin/hackathon-23-2/blob/main/src/main/java/com/vaadin/example/sightseeing/data/service/OverpassService.java#L27)
   - you can use this [frontend](https://overpass-turbo.eu/) for playing with querys
- To reset current database and re-run data generator, remove database file `rm -f ./h2-app.mv.db`
- Map documentation
  - [flow component](https://vaadin.com/docs/latest/components/map)
  - [web component](https://cdn-origin.vaadin.com/vaadin-web-components/23.2.0/index.html#/elements/vaadin-map)

