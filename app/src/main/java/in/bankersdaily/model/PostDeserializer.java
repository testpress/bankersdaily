package in.bankersdaily.model;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import in.bankersdaily.network.ApiClient;

/**
 * Handle nested rendered json fields
 * Replace "main_field": {"rendered": "required text"} as
 * "main_field": "required text"
 */

public class PostDeserializer implements JsonDeserializer<Post> {

    @Override
    public Post deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        Gson gson = ApiClient.getGsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        JsonObject jsonObject = json.getAsJsonObject();
        Post post = gson.fromJson(json, Post.class);
        post.setTitle(getRenderedString(jsonObject, "title"));
        post.setContent(getRenderedString(jsonObject, "content"));

        JsonObject embedded = jsonObject.getAsJsonObject("_embedded");
        if (embedded.has("wp:term")) {
            JsonElement categoriesJson = embedded.getAsJsonArray("wp:term").get(0);
            gson = ApiClient.getGsonBuilder().create();
            Type type = new TypeToken<List<Category>>() {}.getType();
            List<Category> categories = gson.fromJson(categoriesJson, type);
            post.setCategories(categories);
        }
        if (embedded.has("wp:featuredmedia")) {
            JsonObject featuredMedia = embedded.getAsJsonArray("wp:featuredmedia")
                    .get(0).getAsJsonObject();

            JsonObject imageSizes =
                    featuredMedia.getAsJsonObject("media_details").getAsJsonObject("sizes");

            String imageUrl = featuredMedia.get("source_url").getAsString();
            post.setFeaturedMedia(imageUrl);
            if (imageSizes.has("gazana_mini")) {
                imageUrl = imageSizes.getAsJsonObject("gazana_mini").get("source_url").getAsString();
            }
            post.setFeaturedMediaSquare(imageUrl);
        }
        return post;
    }

    private String getRenderedString(JsonObject jsonObject, String key) {
        return jsonObject.getAsJsonObject(key).get("rendered").getAsString();
    }
}
