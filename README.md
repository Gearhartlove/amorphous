note : following is inspired by Jason Gregory chapter 5 Engine Support Systems 5.4.4 Localization
# genesis
- I believe game development is a collaborative effort.
- There are too many hats for any solo dev to wear.
- Text in a video game is like an urban sprawl, it's everywhere.
- A game needs to be accessible.

Combining all the above, I set out to make a text localization tool. Localization is 'the process of conforming software and the digital user experience to the language and cultural norms of an end user in any geographic region'. This is a big topic, so I focused on text.

I needed a system that fulfilled the following requirements : 
- easy to modify translations
- easy to view text assets
- searchable asset based on description or name
- easy to reference in game code
- hot reload text asset changes

Here is a long form video of me doing all of those things. On the right is my bevy video game and the left has my translation web application. 

![Demo Video](readme-assets/demo2.webm)

## tech stack highlights
- **backend**
  - javalin : simple + productive web server
  - **templating**
      - mustache.java : fantastic logicless template that just works
  - **build tool**
    - gradle : created simple run configuration
- **frontend**
  - html + css : visual content
  - htmx : ajax web requests (no JS needed)
    - search pattern to filter assets
- **database**
  - SQLite : embedded + easy to use 
- **game engine**
  - Bevy : a refreshingly simple data-driven game engine built in Rust
  - multi-threading : spawned database-polling thread to update assets while game is running 
