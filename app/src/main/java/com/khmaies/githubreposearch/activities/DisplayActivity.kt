package com.khmaies.githubreposearch.activities

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import com.khmaies.githubreposearch.R
import com.khmaies.githubreposearch.adapters.DisplayAdapter
import com.khmaies.githubreposearch.app.Constants
import com.khmaies.githubreposearch.extensions.showErrorMessage
import com.khmaies.githubreposearch.extensions.toast
import com.khmaies.githubreposearch.models.Repository
import com.khmaies.githubreposearch.models.SearchResponse
import com.khmaies.githubreposearch.retrofit.GithubAPIService
import com.khmaies.githubreposearch.retrofit.RetrofitClient
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_display.*
import kotlinx.android.synthetic.main.header.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class DisplayActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var displayAdapter: DisplayAdapter
    private var browsedRepositories: List<Repository> = mutableListOf()
    private val githubAPIService: GithubAPIService by lazy {
        RetrofitClient.githubAPIService
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)

        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle("Showing Browsed Results")

        setAppUserName()
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager


        navigationView.setNavigationItemSelectedListener(this)
        val drawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        val intent = intent
        if (intent.getIntExtra(Constants.KEY_QUERY_TYPE, -1) == Constants.SEARCH_BY_REPO) {
            val queryRepo = intent.getStringExtra(Constants.KEY_REPO_SEARCH)
            val repoLanguage = intent.getStringExtra(Constants.KEY_LANGUAGE)
            fetchRepositories(queryRepo, repoLanguage)
        } else {
            val githubUser = intent.getStringExtra(Constants.KEY_GITHUB_USER)
            fetchUserRepositories(githubUser)
        }
    }

    private fun setAppUserName() {

        val sp = getSharedPreferences(Constants.APP_SHARED_PREFERENCES, MODE_PRIVATE)
        val userName = sp.getString(Constants.KEY_PERSON_NAME, "User")
        val headerView = navigationView.getHeaderView(0)
        headerView.txvName.text = userName

    }

    private fun fetchUserRepositories(githubUser: String) {

        githubAPIService.searchRepositoriesByUser(githubUser).enqueue(object : Callback<List<Repository>>{
            override fun onResponse(call: Call<List<Repository>>?, response: Response<List<Repository>>) {

                if(response.isSuccessful){
                    Log.i(TAG, "posts loaded from API $response")


                    response.body()?.let {
                        browsedRepositories = it
                    }

                    if(browsedRepositories.isNotEmpty()){
                        setupRecyclerView(browsedRepositories)
                    } else {
                        toast("No Items Found")
                    }
                }else {
                    Log.i(TAG, "Error $response")
                    showErrorMessage(response.errorBody()!!)
                }
            }

            override fun onFailure(call: Call<List<Repository>>?, t: Throwable) {

                toast(t.toString(),Toast.LENGTH_LONG)

            }
        })
    }

    private fun fetchRepositories(queryRepo: String, repoLanguage: String) {
        var queryRepo = queryRepo
        val query: MutableMap<String, String> = HashMap()

        if (repoLanguage.isNotEmpty())
            queryRepo += " language:$repoLanguage"

        query["q"] = queryRepo

        githubAPIService.searchRepositories(query).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {

                if (response.isSuccessful) {
                    Log.i(TAG, "posts loaded from API $response")

                            response.body()?.items?.let {

                                browsedRepositories = it
                            }

                    if (browsedRepositories.isNotEmpty())
                        setupRecyclerView(browsedRepositories)
                    else toast("No Items Found")
                } else {
                    Log.i(TAG, "error $response")
                    showErrorMessage(response.errorBody()!!)
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                toast(t.toString())
            }
        })
    }

    private fun setupRecyclerView(items: List<Repository>) {
        displayAdapter = DisplayAdapter(this, items)
        recyclerView.adapter = displayAdapter
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {

        menuItem.isChecked = true

        when (menuItem.itemId) {

            R.id.item_bookmark -> {consumeMenuEvent({ showBookmarks() }, "Showing Bookmarks")}

            R.id.item_browsed_results -> {consumeMenuEvent({ showBrowsedResults() },"showing Browsed Results")}
        }
        return true
    }

   private inline fun consumeMenuEvent(myFunc: () -> Unit, title: String){
        myFunc()
        closeDrawer()
        supportActionBar!!.title = title
    }

    private fun showBrowsedResults() {
        displayAdapter.swap(browsedRepositories)
    }

    private fun showBookmarks() {

        val realm = Realm.getDefaultInstance()

        realm.executeTransaction{ realm ->
            val bookmarkedRepoList = realm.where(Repository::class.java).findAll()
            displayAdapter.swap(bookmarkedRepoList)
        }
    }

    private fun closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) closeDrawer() else {
            super.onBackPressed()
        }
    }

    companion object {
        private val TAG = DisplayActivity::class.java.simpleName
    }
}