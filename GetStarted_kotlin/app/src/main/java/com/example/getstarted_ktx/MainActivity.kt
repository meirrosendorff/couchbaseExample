package com.example.getstarted_ktx

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.couchbase.lite.*
import java.net.URI


class MainActivity : AppCompatActivity() {

    private var TAG = "CBL-GS"
    private val textView: TextView by lazy { findViewById(R.id.mainTextView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Not Working
        val username = "407dd1a8-f2ff-4286-8eba-72e9026d4bb2"
        val password = "805de090-494d-474e-967d-db94bfb6d735"

        // Working
//        val username = "edaa9567-e315-4ef9-944e-0aa9ebda2bcf"
//        val password = "933acd94-b045-43ce-b8aa-cdeb9586e6e9"


        // initialize couchbase
        CouchbaseLite.init(this)
        Database.log.console.domains = LogDomain.ALL_DOMAINS
        Database.log.console.level = LogLevel.VERBOSE
        Log.i(TAG,"Initialized CBL")

        // Create a database
        Log.i(TAG, "Starting DB")
        val cfg = DatabaseConfigurationFactory.create()
        val database = Database(  "link", cfg)


        // Save Document
        // commenting out this line will make it sync correctly for the Not Working Credentials
        saveFirstDocumentToDatabase(username, database)


        startReplicator(username, password, database)
    }

    private fun saveFirstDocumentToDatabase(username: String, database: Database) {
        val meta = mutableMapOf<String, Any>()
        meta["type"] = "test"
        meta["sync"] = listOf("device:$username:device")

        val payload = mutableMapOf<String, Any>()
        payload["id"] = "123456"

        val mutableDoc = MutableDocument()
        mutableDoc.setValue("payload", payload)
        mutableDoc.setValue("meta", meta)

        database.save(mutableDoc)
    }

    private fun startReplicator(username: String, password: String, database: Database) {
        val replicator = Replicator(
            ReplicatorConfigurationFactory.create(
                database = database,
                target = URLEndpoint(URI("ws://couchgateway.trackmatic.co.za:4984/link")),
                type = ReplicatorType.PUSH_AND_PULL,
                authenticator = BasicAuthenticator(username, password.toCharArray()),
                continuous = false
            )
        )

        replicator.addDocumentReplicationListener {
            textView.text = "Total Documents: ${database.count}"
        }

        replicator.addChangeListener { change ->
            val err = change.status.error
            if (err != null) {
                Log.i(TAG, "Error code ::  ${err.code}")
            }

            if (change.status.activityLevel == ReplicatorActivityLevel.STOPPED) {
                Log.i(TAG, "Total documents in database :: ${database.count}")
            }
        }

        replicator.start()
    }
}

