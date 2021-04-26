package br.kimurashin.netflixremake.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import br.kimurashin.netflixremake.model.Category;
import br.kimurashin.netflixremake.model.Movie;

public class CategoryTask extends AsyncTask<String, Void, List<Category>> {

    private final WeakReference<Context> context;
    private ProgressDialog dialog;
    private CategoryLoader categoryLoader;

    public CategoryTask(Context context){ // Constructor
        this.context = new WeakReference<>(context);
    }

    public void setCategoryLoader(CategoryLoader categoryLoader){
        this.categoryLoader = categoryLoader;
    }

    // Starting threading
    // Pre execution
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Context context = this.context.get();
        dialog = ProgressDialog.show(context,"Carregando", "", true);
    }

    // Execution
    @Override
    protected List<Category> doInBackground(String... params) {
        String url = params[0]; // Argument of execute function

        try {
            URL requestUrl = new URL(url); // Receiving url

            // Creating https connection (ssl protocol
            HttpsURLConnection urlConnection = (HttpsURLConnection) requestUrl.openConnection();
            urlConnection.setReadTimeout(2000); // Setting 2s for reading timeout
            urlConnection.setConnectTimeout(2000); // Setting 2s for connection timeout

            int responseCode = urlConnection.getResponseCode(); // Receiving http code
            if (responseCode > 400) { // error, bigger than 400
                throw new IOException("Error na comunicação do servidor");
            }

            // content of request
            InputStream inputStream = urlConnection.getInputStream();
            BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
            // Converting to String
            String jsonAsString = toString(in);
            // Formatting input stream to JSON
            List<Category> categories = getCategories(new JSONObject((jsonAsString)));
            in.close(); // Closing connection
            return categories; // Returning array list
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Post execution and giving results of execution
    @Override
    protected void onPostExecute(List<Category> categories) {
        super.onPostExecute(categories);
        dialog.dismiss();

        //LISTENER
        if (categoryLoader != null){
            categoryLoader.onResult(categories);
        }
    }

    private List<Category> getCategories(JSONObject json) throws JSONException {
        List<Category> categories = new ArrayList<>(); // Array list to return

        JSONArray categoryArray = json.getJSONArray("category"); // Getting category object
        for (int i = 0; i < categoryArray.length(); i++) { // looping into the object
            JSONObject category = categoryArray.getJSONObject(i); // getting object of index i
            String title = category.getString("title"); // getting title object

            List<Movie> movies = new ArrayList<>();
            JSONArray movieArray = category.getJSONArray("movie"); // getting movie object
            for (int j = 0; j < movieArray.length(); j++) {
                JSONObject movie = movieArray.getJSONObject(j); // looping into the obj

                String coverUrl = movie.getString("cover_url"); // getting url of cover
                int id = movie.getInt("id");

                Movie movieObj = new Movie();
                movieObj.setCoverUrl(coverUrl);
                movieObj.setId(id);
                movies.add(movieObj);
            }
            Category categoryObj = new Category();
            categoryObj.setName(title);
            categoryObj.setMovies(movies);
            categories.add(categoryObj);
        }
        // Returning array list with all categories with it's movies inside.
        return categories;
    }

    private String toString(InputStream is) throws IOException {
        byte[] bytes = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int lidos;
        while((lidos = is.read(bytes))>0){
            baos.write(bytes, 0, lidos);
        }

        return new String(baos.toByteArray());
    }

    public interface CategoryLoader{
        void onResult(List<Category> categories);
    }
}
